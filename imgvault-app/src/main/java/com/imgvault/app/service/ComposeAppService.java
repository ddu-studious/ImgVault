package com.imgvault.app.service;

import com.imgvault.common.dto.*;
import com.imgvault.domain.entity.ImageEntity;
import com.imgvault.domain.repository.ImageRepository;
import com.imgvault.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片合成应用服务
 * 编排合成引擎、模板引擎、存储和持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComposeAppService {

    private final ImageComposeService composeService;
    private final ComposeTemplateEngine templateEngine;
    private final MinioStorageService storageService;
    private final ImageRepository imageRepository;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 自由画布合成
     */
    public ComposeResultDTO compose(ComposeRequest request, String visitorId) {
        log.info("开始图片合成: canvas={}x{}, layers={}",
                request.getCanvas().getWidth(), request.getCanvas().getHeight(),
                request.getLayers().size());

        List<ImageComposeService.LayerInput> layerInputs = prepareLayerInputs(request.getLayers());
        ComposeRequest.OutputConfig output = request.getOutput() != null
                ? request.getOutput() : new ComposeRequest.OutputConfig();

        ImageComposeService.ComposeOutput result = composeService.compose(
                request.getCanvas(), layerInputs, output);

        return saveAndBuildResult(result, visitorId, "composed");
    }

    /**
     * 模板合成
     */
    public ComposeResultDTO composeByTemplate(ComposeTemplateRequest request, String visitorId) {
        log.info("模板合成: template={}, images={}", request.getTemplate(), request.getImages().size());

        ComposeRequest composeRequest = templateEngine.buildFromTemplate(request);
        return compose(composeRequest, visitorId);
    }

    /**
     * 获取模板列表
     */
    public List<Map<String, Object>> listTemplates() {
        return templateEngine.listTemplates().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.id);
            m.put("name", t.name);
            m.put("description", t.description);
            m.put("minImages", t.minImages);
            m.put("maxImages", t.maxImages);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 准备图层输入数据（从 MinIO 拉取源图）
     */
    private List<ImageComposeService.LayerInput> prepareLayerInputs(
            List<ComposeRequest.LayerConfig> layers) {
        List<ImageComposeService.LayerInput> inputs = new ArrayList<>();

        for (ComposeRequest.LayerConfig cfg : layers) {
            ImageComposeService.LayerInput input = new ImageComposeService.LayerInput();
            input.setConfig(cfg);

            if ("image".equals(cfg.getType()) && cfg.getImageId() != null) {
                try {
                    ImageEntity entity = imageRepository.findById(cfg.getImageId());
                    if (entity != null && entity.getStoragePath() != null) {
                        InputStream stream = storageService.getFileStream(entity.getStoragePath());
                        input.setImageData(toByteArray(stream));
                        stream.close();
                    }
                } catch (Exception e) {
                    log.warn("拉取图片数据失败: imageId={}, error={}", cfg.getImageId(), e.getMessage());
                }
            }

            inputs.add(input);
        }

        return inputs;
    }

    /**
     * 保存合成结果到 MinIO 和数据库
     */
    private ComposeResultDTO saveAndBuildResult(ImageComposeService.ComposeOutput result,
                                                String visitorId, String source) {
        String ext = result.getFormat();
        if ("jpeg".equals(ext)) ext = "jpg";
        String storagePath = storageService.generateStoragePath(ext)
                .replace("originals/", "composed/");

        String contentType = "image/" + result.getFormat();
        byte[] data = result.getData();

        storageService.uploadFile(storagePath, new ByteArrayInputStream(data),
                data.length, contentType);

        ImageEntity entity = new ImageEntity();
        entity.setImageUuid(UUID.randomUUID().toString());
        entity.setOriginalName("composed_" + System.currentTimeMillis() + "." + ext);
        entity.setStoragePath(storagePath);
        entity.setBucketName("imgvault");
        entity.setFileSize((long) data.length);
        entity.setWidth(result.getWidth());
        entity.setHeight(result.getHeight());
        entity.setFormat(result.getFormat());
        entity.setMimeType(contentType);
        entity.setUploadSource(source);
        entity.setVisitorId(visitorId);
        entity.setStatus(1);
        entity.setAccessLevel(0);
        entity.setViewCount(0L);
        entity.setCreatedAt(LocalDateTime.now().format(DT_FMT));
        entity.setUpdatedAt(LocalDateTime.now().format(DT_FMT));

        imageRepository.insert(entity);

        String downloadUrl = storageService.getPresignedDownloadUrl(storagePath, 3600);

        ComposeResultDTO dto = new ComposeResultDTO();
        dto.setImageId(entity.getId());
        dto.setImageUuid(entity.getImageUuid());
        dto.setDownloadUrl(downloadUrl);
        dto.setWidth(result.getWidth());
        dto.setHeight(result.getHeight());
        dto.setFileSize((long) data.length);
        dto.setFormat(result.getFormat());

        log.info("合成完成: imageId={}, size={}bytes, path={}",
                entity.getId(), data.length, storagePath);

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
