package com.imgvault.api.config;

import com.imgvault.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * F10: 健康检查配置
 * 包含 SQLite 和 MinIO 健康状态
 */
@Configuration
@RequiredArgsConstructor
public class HealthIndicatorConfig {

    private final MinioStorageService storageService;
    private final DataSource dataSource;

    /**
     * MinIO 健康检查
     */
    @Bean
    public HealthIndicator minioHealthIndicator() {
        return () -> {
            try {
                if (storageService.isHealthy()) {
                    return Health.up()
                            .withDetail("service", "MinIO")
                            .withDetail("status", "connected")
                            .build();
                } else {
                    return Health.down()
                            .withDetail("service", "MinIO")
                            .withDetail("status", "disconnected")
                            .build();
                }
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "MinIO")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }

    /**
     * SQLite 健康检查
     */
    @Bean
    public HealthIndicator sqliteHealthIndicator() {
        return () -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA journal_mode");
                String journalMode = rs.next() ? rs.getString(1) : "unknown";

                rs = stmt.executeQuery("SELECT COUNT(*) FROM img_image");
                long imageCount = rs.next() ? rs.getLong(1) : 0;

                return Health.up()
                        .withDetail("service", "SQLite")
                        .withDetail("journalMode", journalMode)
                        .withDetail("imageCount", imageCount)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "SQLite")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        };
    }
}
