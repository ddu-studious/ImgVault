package com.imgvault.app.service;

import com.imgvault.common.constant.StorageConstants;
import com.imgvault.common.dto.*;
import com.imgvault.common.enums.ImageFormat;
import com.imgvault.common.enums.ImageStatus;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.common.util.FileHashUtil;
import com.imgvault.common.util.MagicBytesValidator;
import com.imgvault.domain.entity.FileFingerprintEntity;
import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.repository.FileFingerprintRepository;
import com.imgvault.domain.repository.ImageRepository;
import com.imgvault.infrastructure.storage.ImgproxyService;
import com.imgvault.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * 图片应用服务
 * 负责业务编排: 安全校验 → 哈希计算 → 存储 → 元数据提取 → 保存记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAppService {

    private final ImageRepository imageRepository;
    private final FileFingerprintRepository fingerprintRepository;
    private final MinioStorageService storageService;
    private final ImgproxyService imgproxyService;

    /**
     * F02 + F03 + F08: 图片上传（含安全校验和元数据提取）
     */
    public ImageUploadDTO uploadImage(MultipartFile file) {
        // 1. 基本参数校验
        validateUploadFile(file);

        try {
            byte[] fileBytes = file.getBytes();

            // 2. Magic Bytes 安全校验 (F03)
            validateMagicBytes(fileBytes, file.getContentType());

            // 3. 计算文件哈希 (SHA-256 + MD5)
            String fileHash = FileHashUtil.sha256(fileBytes);
            String fileMd5 = FileHashUtil.md5(fileBytes);

            // 4. 检测 MIME 类型和格式
            String mimeType = MagicBytesValidator.detectMimeType(fileBytes);
            if (mimeType == null) {
                throw BusinessException.badRequest("无法检测文件类型");
            }
            ImageFormat imageFormat = ImageFormat.fromMimeType(mimeType);

            // 5. 提取图片尺寸信息 (F08)
            int width = 0, height = 0;
            boolean hasAlpha = false;
            String colorSpace = null;
            try {
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(fileBytes));
                if (bufferedImage != null) {
                    width = bufferedImage.getWidth();
                    height = bufferedImage.getHeight();
                    hasAlpha = bufferedImage.getColorModel().hasAlpha();
                    colorSpace = bufferedImage.getColorModel().getColorSpace().getType() == 5 ? "RGB" : "OTHER";
                }
            } catch (Exception e) {
                log.warn("图片尺寸提取失败: {}", e.getMessage());
            }

            // 6. 生成存储路径并上传到 MinIO（字节流直写，零重编码）
            String extension = imageFormat.getExtensions()[0].substring(1);
            String storagePath = storageService.generateStoragePath(extension);
            storageService.uploadFile(storagePath, new ByteArrayInputStream(fileBytes),
                    fileBytes.length, mimeType);

            // 7. 保存图片记录
            ImageEntity entity = new ImageEntity();
            entity.setImageUuid(UUID.randomUUID().toString());
            entity.setFileHash(fileHash);
            entity.setFileMd5(fileMd5);
            entity.setOriginalName(file.getOriginalFilename());
            entity.setStoragePath(storagePath);
            entity.setBucketName(StorageConstants.DEFAULT_BUCKET);
            entity.setFileSize((long) fileBytes.length);
            entity.setWidth(width);
            entity.setHeight(height);
            entity.setFormat(imageFormat.getFormat());
            entity.setMimeType(mimeType);
            entity.setColorSpace(colorSpace);
            entity.setHasAlpha(hasAlpha ? 1 : 0);
            entity.setStatus(ImageStatus.NORMAL.getCode());
            entity.setAccessLevel(0);
            imageRepository.insert(entity);

            // 8. 保存文件指纹（用于秒传）
            FileFingerprintEntity fingerprint = new FileFingerprintEntity();
            fingerprint.setFileHash(fileHash);
            fingerprint.setFileMd5(fileMd5);
            fingerprint.setStoragePath(storagePath);
            fingerprint.setFileSize((long) fileBytes.length);
            try {
                fingerprintRepository.insert(fingerprint);
            } catch (Exception e) {
                // 指纹已存在时忽略
                log.debug("文件指纹已存在: hash={}", fileHash);
            }

            // 9. 构建返回结果
            ImageUploadDTO dto = new ImageUploadDTO();
            dto.setId(entity.getId());
            dto.setImageUuid(entity.getImageUuid());
            dto.setOriginalName(entity.getOriginalName());
            dto.setFileSize(entity.getFileSize());
            dto.setWidth(width);
            dto.setHeight(height);
            dto.setFormat(imageFormat.getFormat());
            dto.setMimeType(mimeType);
            dto.setStoragePath(storagePath);
            dto.setFileHash(fileHash);
            dto.setDownloadUrl(storageService.getPresignedDownloadUrl(storagePath,
                    StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS));

            log.info("图片上传成功: id={}, uuid={}, path={}", entity.getId(), entity.getImageUuid(), storagePath);
            return dto;

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("图片上传IO异常", e);
            throw new BusinessException("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * F02: 批量上传
     */
    public List<ImageUploadDTO> batchUpload(MultipartFile[] files) {
        List<ImageUploadDTO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                results.add(uploadImage(file));
            } catch (Exception e) {
                log.error("批量上传失败: file={}, error={}", file.getOriginalFilename(), e.getMessage());
                // 继续上传其他文件
            }
        }
        return results;
    }

    /**
     * F04: 按 ID 查询图片
     */
    @Cacheable(value = "imageCache", key = "'id:' + #id")
    public ImageDetailDTO getImageById(Long id) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null || entity.getStatus() == ImageStatus.DELETED.getCode()) {
            throw BusinessException.notFound("图片不存在: " + id);
        }
        return convertToDetailDTO(entity);
    }

    /**
     * F04: 按 UUID 查询图片
     */
    @Cacheable(value = "imageCache", key = "'uuid:' + #uuid")
    public ImageDetailDTO getImageByUuid(String uuid) {
        ImageEntity entity = imageRepository.findByUuid(uuid);
        if (entity == null || entity.getStatus() == ImageStatus.DELETED.getCode()) {
            throw BusinessException.notFound("图片不存在: " + uuid);
        }
        return convertToDetailDTO(entity);
    }

    /**
     * F05: 分页查询图片列表
     */
    public PageResult<ImageDetailDTO> listImages(ImageQueryRequest request) {
        Integer status = ImageStatus.NORMAL.getCode();
        String format = StringUtils.isNotBlank(request.getFormat()) ? request.getFormat() : null;

        long total = imageRepository.count(format, status);
        if (total == 0) {
            return PageResult.empty(request.getPage(), request.getSize());
        }

        List<ImageEntity> entities = imageRepository.findPage(
                format, status,
                request.getSortBy(), request.getSortOrder(),
                request.getOffset(), request.getSize());

        List<ImageDetailDTO> dtos = new ArrayList<>();
        for (ImageEntity entity : entities) {
            dtos.add(convertToDetailDTO(entity));
        }

        return PageResult.of(dtos, total, request.getPage(), request.getSize());
    }

    /**
     * F06: 获取图片下载 URL
     */
    public String getDownloadUrl(Long id) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null || entity.getStatus() == ImageStatus.DELETED.getCode()) {
            throw BusinessException.notFound("图片不存在: " + id);
        }
        // 增加浏览次数
        imageRepository.incrementViewCount(id);
        return storageService.getPresignedDownloadUrl(entity.getStoragePath(),
                StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS);
    }

    /**
     * F07: 软删除图片
     */
    @CacheEvict(value = "imageCache", allEntries = true)
    public void softDeleteImage(Long id) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("图片不存在: " + id);
        }
        imageRepository.softDelete(id);
        log.info("图片已软删除: id={}", id);
    }

    /**
     * F07: 物理删除图片（同时删除 MinIO 文件）
     */
    @CacheEvict(value = "imageCache", allEntries = true)
    public void hardDeleteImage(Long id) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("图片不存在: " + id);
        }

        // 删除 MinIO 文件
        try {
            storageService.deleteFile(entity.getStoragePath());
        } catch (Exception e) {
            log.error("MinIO 文件删除失败: path={}", entity.getStoragePath(), e);
        }

        // 删除数据库记录
        imageRepository.deleteById(id);

        // 减少指纹引用
        FileFingerprintEntity fingerprint = fingerprintRepository.findByHash(entity.getFileHash());
        if (fingerprint != null) {
            fingerprintRepository.decrementRefCount(fingerprint.getId());
        }

        log.info("图片已物理删除: id={}, path={}", id, entity.getStoragePath());
    }

    // ==================== Phase 2: imgproxy 图片处理 ====================

    /**
     * F12-F17: 获取图片处理 URL（通用入口）
     * Java 服务生成签名后的 imgproxy URL，302 重定向给客户端
     */
    public String getProcessedImageUrl(Long id, ImageProcessRequest request) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null || entity.getStatus() == ImageStatus.DELETED.getCode()) {
            throw BusinessException.notFound("图片不存在: " + id);
        }
        return imgproxyService.getProcessedUrl(
                entity.getStoragePath(),
                request.getWidth(),
                request.getHeight(),
                request.getFormat(),
                request.getQuality(),
                request.isSmartCrop());
    }

    // ==================== 私有方法 ====================

    /**
     * 基本参数校验
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的文件");
        }
        if (file.getSize() > StorageConstants.MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小超过限制(50MB)");
        }
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !ImageFormat.isAllowed(contentType)) {
            throw BusinessException.badRequest("不支持的文件类型: " + contentType);
        }
    }

    /**
     * Magic Bytes 安全校验
     */
    private void validateMagicBytes(byte[] fileBytes, String claimedMimeType) {
        byte[] header = Arrays.copyOf(fileBytes, Math.min(fileBytes.length, 16));
        if (!MagicBytesValidator.isValidImage(header)) {
            throw BusinessException.badRequest("文件内容不是合法的图片格式");
        }
        String detectedMimeType = MagicBytesValidator.detectMimeType(header);
        if (detectedMimeType != null && !detectedMimeType.equals(claimedMimeType)) {
            log.warn("MIME 类型不匹配: claimed={}, detected={}", claimedMimeType, detectedMimeType);
            // 以检测到的为准，不抛异常
        }
    }

    /**
     * 实体转 DTO
     */
    private ImageDetailDTO convertToDetailDTO(ImageEntity entity) {
        ImageDetailDTO dto = new ImageDetailDTO();
        dto.setId(entity.getId());
        dto.setImageUuid(entity.getImageUuid());
        dto.setOriginalName(entity.getOriginalName());
        dto.setFileSize(entity.getFileSize());
        dto.setWidth(entity.getWidth());
        dto.setHeight(entity.getHeight());
        dto.setFormat(entity.getFormat());
        dto.setMimeType(entity.getMimeType());
        dto.setColorSpace(entity.getColorSpace());
        dto.setHasAlpha(entity.getHasAlpha() != null && entity.getHasAlpha() == 1);
        dto.setStatus(entity.getStatus());
        dto.setAccessLevel(entity.getAccessLevel());
        dto.setViewCount(entity.getViewCount());
        dto.setDescription(entity.getDescription());
        dto.setFileHash(entity.getFileHash());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // 生成预签名下载 URL
        try {
            dto.setDownloadUrl(storageService.getPresignedDownloadUrl(
                    entity.getStoragePath(), StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS));
        } catch (Exception e) {
            log.warn("生成下载 URL 失败: path={}", entity.getStoragePath());
        }

        // 生成 imgproxy 缩略图 URL (Phase 2: F12)
        try {
            ImgproxyService.ThumbnailUrls thumbs = imgproxyService.getThumbnailUrls(entity.getStoragePath());
            Map<String, String> thumbnails = new LinkedHashMap<>();
            thumbnails.put("small", thumbs.getSmall());
            thumbnails.put("medium", thumbs.getMedium());
            thumbnails.put("large", thumbs.getLarge());
            dto.setThumbnails(thumbnails);

            // 格式变体 URL (Phase 2: F14)
            Map<String, String> variants = new LinkedHashMap<>();
            variants.put("webp", imgproxyService.getWebpUrl(entity.getStoragePath(), 85));
            variants.put("avif", imgproxyService.getAvifUrl(entity.getStoragePath(), 80));
            dto.setVariants(variants);
        } catch (Exception e) {
            log.warn("生成 imgproxy URL 失败: path={}", entity.getStoragePath());
        }

        return dto;
    }
}
