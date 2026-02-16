package com.imgvault.app.service;

import com.imgvault.common.constant.StorageConstants;
import com.imgvault.common.dto.*;
import com.imgvault.common.enums.*;
import com.imgvault.common.exception.BusinessException;
import com.imgvault.common.util.FileHashUtil;
import com.imgvault.common.util.MagicBytesValidator;
import com.imgvault.domain.entity.*;
import com.imgvault.domain.repository.*;
import com.imgvault.infrastructure.storage.ImgproxyService;
import com.imgvault.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片应用服务
 * 负责业务编排: 安全校验 → 哈希计算 → 存储 → 元数据提取 → 保存记录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageAppService {

    private final ImageRepository imageRepository;
    private final ImageMetadataRepository imageMetadataRepository;
    private final FileFingerprintRepository fingerprintRepository;
    private final UploadTaskRepository uploadTaskRepository;
    private final AsyncTaskRepository asyncTaskRepository;
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

            // 3.5 文件指纹去重检查
            FileFingerprintEntity existingFingerprint = fingerprintRepository.findByHash(fileHash);
            if (existingFingerprint != null) {
                ImageEntity existingImage = imageRepository.findByHash(fileHash);
                if (existingImage != null && existingImage.getStatus() == ImageStatus.NORMAL.getCode()) {
                    log.info("检测到重复文件上传: hash={}, existingId={}", fileHash, existingImage.getId());
                    ImageUploadDTO dto = new ImageUploadDTO();
                    dto.setId(existingImage.getId());
                    dto.setImageUuid(existingImage.getImageUuid());
                    dto.setOriginalName(existingImage.getOriginalName());
                    dto.setFileSize(existingImage.getFileSize());
                    dto.setWidth(existingImage.getWidth());
                    dto.setHeight(existingImage.getHeight());
                    dto.setFormat(existingImage.getFormat());
                    dto.setMimeType(existingImage.getMimeType());
                    dto.setStoragePath(existingImage.getStoragePath());
                    dto.setFileHash(fileHash);
                    dto.setDownloadUrl(storageService.getPresignedDownloadUrl(
                            existingImage.getStoragePath(), StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS));
                    dto.setDuplicate(true);
                    return dto;
                }
            }

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
     * 支持按状态、格式、关键词过滤
     */
    public PageResult<ImageDetailDTO> listImages(ImageQueryRequest request) {
        // 状态：默认只查正常(1)，admin可传指定状态或null查全部
        Integer status = request.getStatus() != null ? request.getStatus() : ImageStatus.NORMAL.getCode();
        String format = StringUtils.isNotBlank(request.getFormat()) ? request.getFormat() : null;
        String keyword = StringUtils.isNotBlank(request.getKeyword()) ? request.getKeyword() : null;

        long total = imageRepository.count(format, status, keyword);
        if (total == 0) {
            return PageResult.empty(request.getPage(), request.getSize());
        }

        List<ImageEntity> entities = imageRepository.findPage(
                format, status, keyword,
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
     * F30: 从回收站恢复图片
     */
    @CacheEvict(value = "imageCache", allEntries = true)
    public void restoreImage(Long id) {
        ImageEntity entity = imageRepository.findById(id);
        if (entity == null) {
            throw BusinessException.notFound("图片不存在: " + id);
        }
        if (entity.getStatus() != ImageStatus.DELETED.getCode()) {
            throw BusinessException.badRequest("该图片未被删除，无需恢复");
        }
        imageRepository.updateStatus(id, ImageStatus.NORMAL.getCode());
        log.info("图片已恢复: id={}", id);
    }

    /**
     * Admin: 获取系统统计信息
     */
    public java.util.Map<String, Object> getAdminStats() {
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalImages", imageRepository.countByStatus(ImageStatus.NORMAL.getCode()));
        stats.put("deletedImages", imageRepository.countByStatus(ImageStatus.DELETED.getCode()));
        stats.put("reviewingImages", imageRepository.countByStatus(ImageStatus.REVIEWING.getCode()));
        stats.put("totalStorage", imageRepository.sumFileSize());
        stats.put("todayUploads", imageRepository.countTodayUploads());

        // 格式分布
        java.util.Map<String, Long> formatDist = new java.util.LinkedHashMap<>();
        for (java.util.Map<String, Object> row : imageRepository.countByFormat()) {
            String format = String.valueOf(row.get("format"));
            long cnt = ((Number) row.get("cnt")).longValue();
            formatDist.put(format, cnt);
        }
        stats.put("formatDistribution", formatDist);
        return stats;
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

    // ==================== Phase 3: 高级上传功能 ====================

    /**
     * F18: 秒传检测
     * 根据 SHA-256 + MD5 双重哈希检测文件是否已存在
     */
    @Cacheable(value = "fingerprintCache", key = "#request.fileHash + ':' + #request.fileMd5")
    public InstantUploadDTO checkInstantUpload(InstantUploadRequest request) {
        // 先通过 SHA-256 查找指纹
        FileFingerprintEntity fingerprint = fingerprintRepository.findByHash(request.getFileHash());
        if (fingerprint == null) {
            return InstantUploadDTO.notMatched();
        }

        // 双重校验: MD5 + 文件大小
        if (!fingerprint.getFileMd5().equals(request.getFileMd5())) {
            log.warn("SHA-256 匹配但 MD5 不匹配: hash={}", request.getFileHash());
            return InstantUploadDTO.notMatched();
        }
        if (!fingerprint.getFileSize().equals(request.getFileSize())) {
            log.warn("哈希匹配但文件大小不同: hash={}, expected={}, actual={}",
                    request.getFileHash(), fingerprint.getFileSize(), request.getFileSize());
            return InstantUploadDTO.notMatched();
        }

        // 秒传成功 — 创建新的图片记录引用同一个存储文件
        ImageEntity entity = new ImageEntity();
        entity.setImageUuid(UUID.randomUUID().toString());
        entity.setFileHash(request.getFileHash());
        entity.setFileMd5(request.getFileMd5());
        entity.setOriginalName(request.getOriginalName());
        entity.setStoragePath(fingerprint.getStoragePath());
        entity.setBucketName(StorageConstants.DEFAULT_BUCKET);
        entity.setFileSize(request.getFileSize());
        entity.setStatus(ImageStatus.NORMAL.getCode());
        entity.setAccessLevel(0);
        imageRepository.insert(entity);

        // 增加指纹引用计数
        fingerprintRepository.incrementRefCount(fingerprint.getId());

        String downloadUrl = null;
        try {
            downloadUrl = storageService.getPresignedDownloadUrl(
                    fingerprint.getStoragePath(), StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS);
        } catch (Exception e) {
            log.warn("秒传后生成下载 URL 失败: {}", e.getMessage());
        }

        // 异步提取 EXIF (F22)
        submitAsyncTask(AsyncTaskType.EXIF_EXTRACT, entity.getId(), null);

        log.info("秒传成功: id={}, uuid={}, hash={}", entity.getId(), entity.getImageUuid(), request.getFileHash());
        return InstantUploadDTO.matched(entity.getId(), entity.getImageUuid(), downloadUrl);
    }

    /**
     * F19: 获取预签名上传 URL（客户端直传 MinIO）
     */
    public PresignedUploadDTO getPresignedUploadUrl(PresignedUploadRequest request) {
        String fileName = request.getFileName();
        String extension = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";

        // 验证文件格式
        try {
            ImageFormat.fromExtension("." + extension);
        } catch (Exception e) {
            throw BusinessException.badRequest("不支持的文件格式: " + extension);
        }

        String storagePath = storageService.generateStoragePath(extension);
        String uploadUrl = storageService.getPresignedUploadUrl(storagePath,
                request.getContentType(), StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS);

        PresignedUploadDTO dto = new PresignedUploadDTO();
        dto.setUploadUrl(uploadUrl);
        dto.setStoragePath(storagePath);
        dto.setExpirySeconds(StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS);
        return dto;
    }

    /**
     * F19: 客户端直传完成后确认，创建图片记录
     */
    public ImageUploadDTO confirmPresignedUpload(PresignedUploadConfirmRequest request) {
        // 验证文件已存在于 MinIO
        // TODO: 可以通过 MinIO statObject 校验

        ImageEntity entity = new ImageEntity();
        entity.setImageUuid(UUID.randomUUID().toString());
        entity.setOriginalName(request.getOriginalName());
        entity.setStoragePath(request.getStoragePath());
        entity.setBucketName(StorageConstants.DEFAULT_BUCKET);
        entity.setFileSize(request.getFileSize());
        entity.setMimeType(request.getContentType());
        entity.setFileHash(request.getFileHash());
        entity.setFileMd5(request.getFileMd5());
        entity.setStatus(ImageStatus.NORMAL.getCode());
        entity.setAccessLevel(0);

        // 根据 MIME 类型推断格式
        if (StringUtils.isNotBlank(request.getContentType())) {
            try {
                ImageFormat format = ImageFormat.fromMimeType(request.getContentType());
                entity.setFormat(format.getFormat());
            } catch (Exception e) {
                log.warn("无法推断图片格式: contentType={}", request.getContentType());
            }
        }

        imageRepository.insert(entity);

        // 保存文件指纹
        if (StringUtils.isNotBlank(request.getFileHash())) {
            FileFingerprintEntity fingerprint = new FileFingerprintEntity();
            fingerprint.setFileHash(request.getFileHash());
            fingerprint.setFileMd5(request.getFileMd5());
            fingerprint.setStoragePath(request.getStoragePath());
            fingerprint.setFileSize(request.getFileSize());
            try {
                fingerprintRepository.insert(fingerprint);
            } catch (Exception e) {
                log.debug("文件指纹已存在: hash={}", request.getFileHash());
            }
        }

        // 异步提取 EXIF (F22)
        submitAsyncTask(AsyncTaskType.EXIF_EXTRACT, entity.getId(), null);

        ImageUploadDTO dto = new ImageUploadDTO();
        dto.setId(entity.getId());
        dto.setImageUuid(entity.getImageUuid());
        dto.setOriginalName(entity.getOriginalName());
        dto.setFileSize(entity.getFileSize());
        dto.setFormat(entity.getFormat());
        dto.setMimeType(entity.getMimeType());
        dto.setStoragePath(entity.getStoragePath());
        dto.setFileHash(entity.getFileHash());

        try {
            dto.setDownloadUrl(storageService.getPresignedDownloadUrl(
                    entity.getStoragePath(), StorageConstants.PRESIGNED_URL_EXPIRY_SECONDS));
        } catch (Exception e) {
            log.warn("生成下载 URL 失败");
        }

        log.info("预签名上传确认成功: id={}, uuid={}", entity.getId(), entity.getImageUuid());
        return dto;
    }

    /**
     * F20: 初始化分片上传
     */
    public ChunkUploadInitDTO initChunkUpload(ChunkUploadInitRequest request) {
        // 检查秒传
        if (StringUtils.isNotBlank(request.getFileHash())) {
            FileFingerprintEntity fingerprint = fingerprintRepository.findByHash(request.getFileHash());
            if (fingerprint != null) {
                log.info("分片上传检测到秒传: hash={}", request.getFileHash());
                // 可以在这里返回秒传结果，但分片上传初始化不直接处理，留给客户端先调用秒传接口
            }
        }

        int chunkSize = (request.getChunkSize() != null && request.getChunkSize() > 0)
                ? request.getChunkSize()
                : StorageConstants.CHUNK_SIZE;
        int totalChunks = (int) Math.ceil((double) request.getFileSize() / chunkSize);

        // 检查是否有未完成的上传任务（断点续传 F21）
        // 使用 fileHash 查找已有任务
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        String extension = request.getFileName().contains(".")
                ? request.getFileName().substring(request.getFileName().lastIndexOf('.') + 1).toLowerCase()
                : "bin";
        String storagePath = storageService.generateStoragePath(extension);

        // 创建上传任务记录
        UploadTaskEntity task = new UploadTaskEntity();
        task.setUploadId(uploadId);
        task.setFileName(request.getFileName());
        task.setFileSize(request.getFileSize());
        task.setFileHash(request.getFileHash());
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setUploadedChunks(0);
        task.setUploadedParts("");
        task.setStoragePath(storagePath);
        task.setStatus(UploadTaskStatus.UPLOADING.getCode());
        // 24小时过期
        task.setExpiresAt(LocalDateTime.now().plusHours(24)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        uploadTaskRepository.insert(task);

        ChunkUploadInitDTO dto = new ChunkUploadInitDTO();
        dto.setUploadId(uploadId);
        dto.setTotalChunks(totalChunks);
        dto.setChunkSize(chunkSize);
        dto.setUploadedChunks(Collections.emptyList());

        log.info("分片上传初始化: uploadId={}, totalChunks={}, chunkSize={}",
                uploadId, totalChunks, chunkSize);
        return dto;
    }

    /**
     * F20 + F21: 上传分片（支持断点续传）
     */
    public ChunkUploadDTO uploadChunk(String uploadId, int chunkNumber, MultipartFile chunkFile) {
        UploadTaskEntity task = uploadTaskRepository.findByUploadId(uploadId);
        if (task == null) {
            throw BusinessException.notFound("上传任务不存在: " + uploadId);
        }
        if (!UploadTaskStatus.UPLOADING.getCode().equals(task.getStatus())) {
            throw BusinessException.badRequest("上传任务状态异常: " + task.getStatus());
        }
        if (chunkNumber < 1 || chunkNumber > task.getTotalChunks()) {
            throw BusinessException.badRequest("无效的分片编号: " + chunkNumber);
        }

        // 检查分片是否已上传（断点续传 F21）
        Set<Integer> uploadedParts = parseUploadedParts(task.getUploadedParts());
        if (uploadedParts.contains(chunkNumber)) {
            ChunkUploadDTO dto = new ChunkUploadDTO();
            dto.setSuccess(true);
            dto.setChunkNumber(chunkNumber);
            dto.setAllUploaded(uploadedParts.size() == task.getTotalChunks());
            return dto;
        }

        try {
            // 上传分片到 MinIO 临时目录
            String chunkPath = String.format("%s/chunks/%s/%04d",
                    StorageConstants.TEMP_PREFIX, uploadId, chunkNumber);
            storageService.uploadFile(chunkPath,
                    chunkFile.getInputStream(), chunkFile.getSize(),
                    "application/octet-stream");

            // 更新任务状态
            uploadedParts.add(chunkNumber);
            task.setUploadedChunks(uploadedParts.size());
            task.setUploadedParts(formatUploadedParts(uploadedParts));
            uploadTaskRepository.update(task);

            ChunkUploadDTO dto = new ChunkUploadDTO();
            dto.setSuccess(true);
            dto.setChunkNumber(chunkNumber);

            // 检查是否全部上传完成
            if (uploadedParts.size() == task.getTotalChunks()) {
                dto.setAllUploaded(true);
                // 触发分片合并 (异步)
                mergeChunksAsync(task);
            } else {
                dto.setAllUploaded(false);
            }

            return dto;

        } catch (IOException e) {
            log.error("分片上传失败: uploadId={}, chunk={}", uploadId, chunkNumber, e);
            throw new BusinessException("分片上传失败: " + e.getMessage());
        }
    }

    /**
     * F21: 查询上传进度（断点续传）
     */
    public ChunkUploadInitDTO getUploadProgress(String uploadId) {
        UploadTaskEntity task = uploadTaskRepository.findByUploadId(uploadId);
        if (task == null) {
            throw BusinessException.notFound("上传任务不存在: " + uploadId);
        }

        ChunkUploadInitDTO dto = new ChunkUploadInitDTO();
        dto.setUploadId(task.getUploadId());
        dto.setTotalChunks(task.getTotalChunks());
        dto.setChunkSize(task.getChunkSize());
        dto.setUploadedChunks(new ArrayList<>(parseUploadedParts(task.getUploadedParts())));
        return dto;
    }

    /**
     * F22: 异步 EXIF 提取（不阻塞上传响应）
     */
    @Async("asyncTaskExecutor")
    public void asyncExtractExif(Long imageId) {
        try {
            ImageEntity image = imageRepository.findById(imageId);
            if (image == null) {
                log.warn("异步 EXIF 提取: 图片不存在 id={}", imageId);
                return;
            }

            // 从 MinIO 获取图片流
            java.io.InputStream imageStream = storageService.getFileStream(image.getStoragePath());
            if (imageStream == null) {
                log.warn("异步 EXIF 提取: 无法获取图片流 path={}", image.getStoragePath());
                return;
            }

            // 提取 EXIF 元数据
            try {
                com.drew.imaging.ImageMetadataReader reader = null;
                com.drew.metadata.Metadata metadata = com.drew.imaging.ImageMetadataReader.readMetadata(imageStream);

                ImageMetadataEntity metaEntity = new ImageMetadataEntity();
                metaEntity.setImageId(imageId);

                // 提取关键 EXIF 字段
                for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
                    if (directory instanceof com.drew.metadata.exif.ExifSubIFDDirectory) {
                        com.drew.metadata.exif.ExifSubIFDDirectory exif =
                                (com.drew.metadata.exif.ExifSubIFDDirectory) directory;
                        metaEntity.setCameraMake(exif.getString(com.drew.metadata.exif.ExifDirectoryBase.TAG_MAKE));
                        metaEntity.setCameraModel(exif.getString(com.drew.metadata.exif.ExifDirectoryBase.TAG_MODEL));
                        if (exif.containsTag(com.drew.metadata.exif.ExifDirectoryBase.TAG_ISO_EQUIVALENT)) {
                            metaEntity.setIso(exif.getInteger(com.drew.metadata.exif.ExifDirectoryBase.TAG_ISO_EQUIVALENT));
                        }
                        metaEntity.setExposureTime(exif.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                        metaEntity.setFNumber(exif.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FNUMBER));
                        metaEntity.setFocalLength(exif.getString(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                        java.util.Date date = exif.getDate(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                        if (date != null) {
                            metaEntity.setDateTaken(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
                        }
                    }

                    if (directory instanceof com.drew.metadata.exif.GpsDirectory) {
                        com.drew.metadata.exif.GpsDirectory gps =
                                (com.drew.metadata.exif.GpsDirectory) directory;
                        com.drew.lang.GeoLocation location = gps.getGeoLocation();
                        if (location != null) {
                            metaEntity.setGpsLatitude(location.getLatitude());
                            metaEntity.setGpsLongitude(location.getLongitude());
                        }
                    }
                }

                // 将完整 EXIF 保存为 JSON
                StringBuilder allMeta = new StringBuilder("{");
                boolean first = true;
                for (com.drew.metadata.Directory directory : metadata.getDirectories()) {
                    for (com.drew.metadata.Tag tag : directory.getTags()) {
                        if (!first) allMeta.append(",");
                        allMeta.append("\"").append(directory.getName()).append(":")
                                .append(tag.getTagName().replace("\"", "\\\""))
                                .append("\":\"")
                                .append(tag.getDescription() != null ? tag.getDescription().replace("\"", "\\\"") : "")
                                .append("\"");
                        first = false;
                    }
                }
                allMeta.append("}");
                metaEntity.setRawExif(allMeta.toString());

                // 保存到数据库
                try {
                    imageMetadataRepository.insert(metaEntity);
                    log.info("EXIF 提取并保存成功: imageId={}, camera={} {}", imageId,
                            metaEntity.getCameraMake(), metaEntity.getCameraModel());
                } catch (Exception ex) {
                    log.warn("EXIF 保存失败（可能已存在）: imageId={}, error={}", imageId, ex.getMessage());
                }

            } finally {
                imageStream.close();
            }

        } catch (Exception e) {
            log.error("异步 EXIF 提取失败: imageId={}", imageId, e);
        }
    }

    /**
     * F23: 提交异步任务到任务表
     */
    public void submitAsyncTask(AsyncTaskType taskType, Long imageId, String params) {
        AsyncTaskEntity task = new AsyncTaskEntity();
        task.setTaskType(taskType.getCode());
        task.setImageId(imageId);
        task.setParams(params);
        task.setStatus(AsyncTaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setMaxRetry(3);
        asyncTaskRepository.insert(task);
        log.debug("异步任务已提交: type={}, imageId={}", taskType.getCode(), imageId);
    }

    // ==================== 分片上传辅助方法 ====================

    /**
     * 异步合并分片
     */
    @Async("asyncTaskExecutor")
    public void mergeChunksAsync(UploadTaskEntity task) {
        try {
            uploadTaskRepository.updateStatus(task.getUploadId(), UploadTaskStatus.MERGING.getCode());

            // 按顺序读取分片并合并上传
            java.io.ByteArrayOutputStream mergedStream = new java.io.ByteArrayOutputStream();
            for (int i = 1; i <= task.getTotalChunks(); i++) {
                String chunkPath = String.format("%s/chunks/%s/%04d",
                        StorageConstants.TEMP_PREFIX, task.getUploadId(), i);
                java.io.InputStream chunkStream = storageService.getFileStream(chunkPath);
                if (chunkStream != null) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = chunkStream.read(buffer)) != -1) {
                        mergedStream.write(buffer, 0, bytesRead);
                    }
                    chunkStream.close();
                }
            }

            byte[] mergedBytes = mergedStream.toByteArray();

            // 验证 Magic Bytes
            if (!MagicBytesValidator.isValidImage(Arrays.copyOf(mergedBytes, Math.min(mergedBytes.length, 16)))) {
                uploadTaskRepository.updateStatus(task.getUploadId(), UploadTaskStatus.FAILED.getCode());
                log.error("分片合并后文件不是有效图片: uploadId={}", task.getUploadId());
                return;
            }

            // 检测 MIME 类型
            String mimeType = MagicBytesValidator.detectMimeType(mergedBytes);
            ImageFormat imageFormat = ImageFormat.fromMimeType(mimeType);

            // 计算哈希
            String fileHash = FileHashUtil.sha256(mergedBytes);
            String fileMd5 = FileHashUtil.md5(mergedBytes);

            // 上传合并后的文件
            storageService.uploadFile(task.getStoragePath(),
                    new ByteArrayInputStream(mergedBytes), mergedBytes.length, mimeType);

            // 提取图片尺寸
            int width = 0, height = 0;
            try {
                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(mergedBytes));
                if (bufferedImage != null) {
                    width = bufferedImage.getWidth();
                    height = bufferedImage.getHeight();
                }
            } catch (Exception e) {
                log.warn("分片合并后提取尺寸失败");
            }

            // 创建图片记录
            ImageEntity entity = new ImageEntity();
            entity.setImageUuid(UUID.randomUUID().toString());
            entity.setOriginalName(task.getFileName());
            entity.setStoragePath(task.getStoragePath());
            entity.setBucketName(StorageConstants.DEFAULT_BUCKET);
            entity.setFileSize((long) mergedBytes.length);
            entity.setFileHash(fileHash);
            entity.setFileMd5(fileMd5);
            entity.setWidth(width);
            entity.setHeight(height);
            entity.setFormat(imageFormat.getFormat());
            entity.setMimeType(mimeType);
            entity.setStatus(ImageStatus.NORMAL.getCode());
            entity.setAccessLevel(0);
            imageRepository.insert(entity);

            // 保存指纹
            FileFingerprintEntity fingerprint = new FileFingerprintEntity();
            fingerprint.setFileHash(fileHash);
            fingerprint.setFileMd5(fileMd5);
            fingerprint.setStoragePath(task.getStoragePath());
            fingerprint.setFileSize((long) mergedBytes.length);
            try {
                fingerprintRepository.insert(fingerprint);
            } catch (Exception e) {
                log.debug("文件指纹已存在");
            }

            // 更新任务状态
            uploadTaskRepository.updateStatus(task.getUploadId(), UploadTaskStatus.COMPLETED.getCode());

            // 异步提取 EXIF
            submitAsyncTask(AsyncTaskType.EXIF_EXTRACT, entity.getId(), null);

            // 清理临时分片
            cleanupChunksAsync(task);

            log.info("分片合并完成: uploadId={}, imageId={}", task.getUploadId(), entity.getId());

        } catch (Exception e) {
            log.error("分片合并失败: uploadId={}", task.getUploadId(), e);
            uploadTaskRepository.updateStatus(task.getUploadId(), UploadTaskStatus.FAILED.getCode());
        }
    }

    /**
     * 清理临时分片文件
     */
    @Async("asyncTaskExecutor")
    public void cleanupChunksAsync(UploadTaskEntity task) {
        try {
            for (int i = 1; i <= task.getTotalChunks(); i++) {
                String chunkPath = String.format("%s/chunks/%s/%04d",
                        StorageConstants.TEMP_PREFIX, task.getUploadId(), i);
                try {
                    storageService.deleteFile(chunkPath);
                } catch (Exception e) {
                    log.debug("清理分片失败: {}", chunkPath);
                }
            }
            log.info("分片清理完成: uploadId={}", task.getUploadId());
        } catch (Exception e) {
            log.warn("分片清理异常: uploadId={}", task.getUploadId());
        }
    }

    private Set<Integer> parseUploadedParts(String parts) {
        if (StringUtils.isBlank(parts)) {
            return new TreeSet<>();
        }
        return Arrays.stream(parts.split(","))
                .filter(StringUtils::isNotBlank)
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private String formatUploadedParts(Set<Integer> parts) {
        return parts.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
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
