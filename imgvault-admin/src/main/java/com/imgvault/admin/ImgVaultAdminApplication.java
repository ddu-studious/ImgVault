package com.imgvault.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ImgVault Admin 管理后台启动类 (Phase 4)
 */
@SpringBootApplication(scanBasePackages = "com.imgvault")
@MapperScan("com.imgvault.infrastructure.persistence.mapper")
public class ImgVaultAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImgVaultAdminApplication.class, args);
    }
}
