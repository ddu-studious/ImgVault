package com.imgvault.infrastructure.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;

    /**
     * 外部访问基础 URL (可选)
     * 用于替换预签名 URL 中的 endpoint，使返回的 URL 通过外部域名代理访问。
     * 例如: https://www.meczyc6.info/imgvault/storage
     * 为空时返回原始 MinIO URL
     */
    private String externalUrl;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}
