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

    /** imgproxy 服务 base URL */
    private String baseUrl = "http://localhost:8081";

    /** HMAC 签名密钥（hex 编码） */
    private String key;

    /** HMAC 签名盐值（hex 编码） */
    private String salt;
}
