package com.imgvault.app.service;

import com.imgvault.common.constant.StorageConstants;
import com.imgvault.common.dto.WatermarkRemoveResultDTO;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.common.util.MagicBytesValidator;
import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.repository.ImageRepository;
import com.imgvault.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

/**
 * 水印去除应用服务
 * 编排引擎选择、图片加载、处理、存储和持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatermarkAppService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String UPLOAD_SOURCE = "watermark-removed";

    private final OpenCvWatermarkService openCvWatermarkService;
    private final IoPaintWatermarkService ioPaintWatermarkService;
    private final MinioStorageService storageService;
    private final ImageRepository imageRepository;

    /**
     * 去除水印
     *
     * @param imageId   已有图片ID（与 file 二选一）
     * @param file      上传的图片文件（与 imageId 二选一）
     * @param maskData  Base64 编码的遮罩 PNG
     * @param engine    引擎: opencv / iopaint，默认 opencv
     * @param visitorId 访客ID
     * @return 处理结果
     */
    public WatermarkRemoveResultDTO removeWatermark(Long imageId, MultipartFile file,
                                                     String maskData, String engine, String visitorId) {
        if (StringUtils.isBlank(maskData)) {
            throw BusinessException.badRequest("遮罩数据不能为空");
        }
        if (imageId == null && (file == null || file.isEmpty())) {
            throw BusinessException.badRequest("请提供 imageId 或上传图片文件");
        }

        String engineName = StringUtils.isNotBlank(engine) ? engine.toLowerCase() : "opencv";
        if (!"opencv".equals(engineName) && !"iopaint".equals(engineName)) {
            throw BusinessException.badRequest("不支持的引擎: " + engine + "，可选: opencv, iopaint");
        }

        byte[] imageBytes = loadImageBytes(imageId, file);
        byte[] maskBytes = decodeBase64Mask(maskData);

        byte[] resultBytes;
        try {
            if ("iopaint".equals(engineName)) {
                resultBytes = ioPaintWatermarkService.removeWatermark(imageBytes, maskBytes);
            } else {
                resultBytes = openCvWatermarkService.removeWatermark(imageBytes, maskBytes);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("水印去除失败: engine={}", engineName, e);
            throw new BusinessException("水印去除失败: " + e.getMessage());
        }

        return saveAndBuildResult(resultBytes, visitorId, engineName);
    }

    private byte[] loadImageBytes(Long imageId, MultipartFile file) {
        if (imageId != null) {
            ImageEntity entity = imageRepository.findById(imageId);
            if (entity == null) {
                throw BusinessException.notFound("图片不存在: " + imageId);
            }
            if (StringUtils.isBlank(entity.getStoragePath())) {
                throw BusinessException.badRequest("图片存储路径为空");
            }
            try {
                InputStream stream = storageService.getFileStream(entity.getStoragePath());
                byte[] bytes = toByteArray(stream);
                stream.close();
                return bytes;
            } catch (Exception e) {
                throw new BusinessException("读取图片失败: " + e.getMessage());
            }
        } else {
            try {
                return file.getBytes();
            } catch (Exception e) {
                throw new BusinessException("读取上传文件失败: " + e.getMessage());
            }
        }
    }

    private byte[] decodeBase64Mask(String maskData) {
        try {
            return Base64.getDecoder().decode(maskData.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("遮罩 Base64 解码失败");
        }
    }

    private WatermarkRemoveResultDTO saveAndBuildResult(byte[] resultBytes, String visitorId, String engine) {
        String mimeType = MagicBytesValidator.detectMimeType(resultBytes);
        if (mimeType == null) {
            mimeType = "image/png";
        }
        String ext = "png";
        if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType)) {
            ext = "jpg";
        } else if ("image/png".equals(mimeType)) {
            ext = "png";
        } else if ("image/webp".equals(mimeType)) {
            ext = "webp";
        }

        String storagePath = storageService.generateStoragePath(ext).replace("originals/", "watermark-removed/");
        String contentType = "image/" + ext;
        if ("jpg".equals(ext)) {
            contentType = "image/jpeg";
        }

        storageService.uploadFile(storagePath, new ByteArrayInputStream(resultBytes),
                resultBytes.length, contentType);

        int width = 0;
        int height = 0;
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(resultBytes));
            if (img != null) {
                width = img.getWidth();
                height = img.getHeight();
            }
        } catch (Exception e) {
            log.warn("提取结果图片尺寸失败: {}", e.getMessage());
        }

        ImageEntity entity = new ImageEntity();
        entity.setImageUuid(UUID.randomUUID().toString());
        entity.setOriginalName("watermark_removed_" + System.currentTimeMillis() + "." + ext);
        entity.setStoragePath(storagePath);
        entity.setBucketName(StorageConstants.DEFAULT_BUCKET);
        entity.setFileSize((long) resultBytes.length);
        entity.setWidth(width);
        entity.setHeight(height);
        entity.setFormat(ext);
        entity.setMimeType(contentType);
        entity.setUploadSource(UPLOAD_SOURCE);
        entity.setVisitorId(visitorId);
        entity.setStatus(1);
        entity.setAccessLevel(0);
        entity.setViewCount(0L);
        entity.setCreatedAt(LocalDateTime.now().format(DT_FMT));
        entity.setUpdatedAt(LocalDateTime.now().format(DT_FMT));

        imageRepository.insert(entity);

        String downloadUrl = storageService.getPresignedDownloadUrl(storagePath, 3600);

        WatermarkRemoveResultDTO dto = new WatermarkRemoveResultDTO();
        dto.setImageId(entity.getId());
        dto.setImageUuid(entity.getImageUuid());
        dto.setDownloadUrl(downloadUrl);
        dto.setWidth(width);
        dto.setHeight(height);
        dto.setFileSize((long) resultBytes.length);
        dto.setFormat(ext);
        dto.setEngine(engine);

        log.info("水印去除完成: imageId={}, engine={}, size={}bytes",
                entity.getId(), engine, resultBytes.length);

        return dto;
    }

    private static byte[] toByteArray(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = input.read(buf)) != -1) {
            buffer.write(buf, 0, len);
        }
        return buffer.toByteArray();
    }
}
