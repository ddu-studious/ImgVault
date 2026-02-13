package com.imgvault.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ImgVault API 服务启动类
 * 统一图片存储与查询服务 - 轻量化混合架构 v2.1.0
 */
@SpringBootApplication(scanBasePackages = "com.imgvault")
@MapperScan("com.imgvault.infrastructure.persistence.mapper")
@EnableAsync
public class ImgVaultApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImgVaultApiApplication.class, args);
    }
}
