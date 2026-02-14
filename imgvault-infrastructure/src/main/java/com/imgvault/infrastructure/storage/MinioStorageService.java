package com.imgvault.infrastructure.storage;

import com.imgvault.common.constant.StorageConstants;
import com.imgvault.infrastructure.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储服务
 * 负责原图直写、预签名 URL 生成、文件删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private volatile boolean minioAvailable = false;

    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                log.info("MinIO Bucket '{}' 已创建", minioConfig.getBucketName());
            } else {
                log.info("MinIO Bucket '{}' 已存在", minioConfig.getBucketName());
            }
            minioAvailable = true;
        } catch (Exception e) {
            log.warn("MinIO 初始化失败（服务将继续启动，但存储功能不可用）: {}", e.getMessage());
            minioAvailable = false;
        }
    }

    /**
     * 确保 MinIO 可用，否则尝试重连
     */
    private void ensureAvailable() {
        if (!minioAvailable) {
            try {
                minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build());
                minioAvailable = true;
                init(); // 重新初始化 bucket
            } catch (Exception e) {
                throw new RuntimeException("MinIO 服务不可用，请确保 MinIO 已启动: " + e.getMessage());
            }
        }
    }

    /**
     * 生成存储路径
     * 格式: originals/2026/02/13/{uuid}.{ext}
     */
    public String generateStoragePath(String extension) {
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("%s/%s/%s.%s",
                StorageConstants.ORIGINALS_PREFIX, datePath, uuid, extension);
    }

    /**
     * 上传文件（字节流直写，零重编码）
     *
     * @param storagePath 存储路径
     * @param inputStream 文件输入流
     * @param fileSize    文件大小
     * @param contentType MIME 类型
     * @return 存储后的 ETag
     */
    public String uploadFile(String storagePath, InputStream inputStream, long fileSize, String contentType) {
        ensureAvailable();
        try {
            ObjectWriteResponse response = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(storagePath)
                    .stream(inputStream, fileSize, -1)
                    .contentType(contentType)
                    .build());
            log.info("文件上传成功: path={}, etag={}", storagePath, response.etag());
            return response.etag();
        } catch (Exception e) {
            log.error("文件上传失败: path={}", storagePath, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名下载 URL
     *
     * @param storagePath 存储路径
     * @param expirySeconds 有效期（秒）
     * @return 预签名 URL
     */
    public String getPresignedDownloadUrl(String storagePath, int expirySeconds) {
        ensureAvailable();
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(storagePath)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名下载 URL 失败: path={}", storagePath, e);
            throw new RuntimeException("生成预签名 URL 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成预签名上传 URL（客户端直传）
     *
     * @param storagePath 存储路径
     * @param expirySeconds 有效期（秒）
     * @return 预签名 URL
     */
    public String getPresignedUploadUrl(String storagePath, int expirySeconds) {
        return getPresignedUploadUrl(storagePath, null, expirySeconds);
    }

    /**
     * 生成预签名上传 URL（客户端直传，含 contentType）
     *
     * @param storagePath 存储路径
     * @param contentType 内容类型
     * @param expirySeconds 有效期（秒）
     * @return 预签名 URL
     */
    public String getPresignedUploadUrl(String storagePath, String contentType, int expirySeconds) {
        ensureAvailable();
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucketName())
                    .object(storagePath)
                    .expiry(expirySeconds, TimeUnit.SECONDS);

            if (contentType != null && !contentType.isEmpty()) {
                java.util.Map<String, String> reqParams = new java.util.HashMap<>();
                reqParams.put("Content-Type", contentType);
                builder.extraQueryParams(reqParams);
            }

            return minioClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            log.error("生成预签名上传 URL 失败: path={}", storagePath, e);
            throw new RuntimeException("生成预签名上传 URL 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     */
    public void deleteFile(String storagePath) {
        ensureAvailable();
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(storagePath)
                    .build());
            log.info("文件已删除: path={}", storagePath);
        } catch (Exception e) {
            log.error("文件删除失败: path={}", storagePath, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件流
     */
    public InputStream getFileStream(String storagePath) {
        ensureAvailable();
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(storagePath)
                    .build());
        } catch (Exception e) {
            log.error("获取文件流失败: path={}", storagePath, e);
            throw new RuntimeException("获取文件流失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查 MinIO 连接健康状态
     */
    public boolean isHealthy() {
        try {
            minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("MinIO 健康检查失败", e);
            return false;
        }
    }
}
