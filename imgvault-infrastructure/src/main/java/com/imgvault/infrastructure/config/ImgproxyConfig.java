package com.imgvault.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * imgproxy 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "imgproxy")
public class ImgproxyConfig {

    /** imgproxy 服务 base URL（内部通信） */
    private String baseUrl = "http://localhost:8081";

    /**
     * imgproxy 外部访问 base URL（可选）
     * 配置后生成的 imgproxy URL 将使用此地址（通过 nginx 代理）
     * 例如: https://www.meczyc6.info/imgvault/imgproxy
     */
    private String externalBaseUrl;

    /** HMAC 签名密钥（hex 编码） */
    private String key;

    /** HMAC 签名盐值（hex 编码） */
    private String salt;
}
