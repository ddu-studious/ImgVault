package com.imgvault.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 数据库初始化
 * 启动时自动执行建表脚本
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SQLiteInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        log.info("初始化 SQLite 数据库...");

        try (Connection conn = dataSource.getConnection()) {
            // 验证 WAL 模式
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA journal_mode");
                if (rs.next()) {
                    log.info("SQLite journal_mode: {}", rs.getString(1));
                }
            }

            // 执行建表脚本
            ClassPathResource resource = new ClassPathResource("db/schema.sql");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    String sql = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                    List<String> statements = parseSqlStatements(sql);
                    try (Statement stmt = conn.createStatement()) {
                        int successCount = 0;
                        for (String s : statements) {
                            try {
                                stmt.execute(s);
                                successCount++;
                            } catch (Exception e) {
                                // 忽略已存在的表/索引/触发器错误
                                if (!e.getMessage().contains("already exists")) {
                                    log.warn("SQL 执行警告: {}", e.getMessage());
                                }
                            }
                        }
                        log.info("SQLite 数据库初始化完成: 成功执行 {} 条语句", successCount);
                    }
                }
            } else {
                log.warn("未找到 db/schema.sql 初始化脚本");
            }
        }
    }

    /**
     * 解析 SQL 文件为独立的语句列表
     * 正确处理 BEGIN...END 块（如 TRIGGER 定义中包含分号的情况）
     */
    private List<String> parseSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideBeginEnd = false;

        String[] lines = sql.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();

            // 跳过空行和纯注释行
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                continue;
            }

            // 跳过 PRAGMA 语句（由连接初始化 SQL 处理）
            if (trimmedLine.toUpperCase().startsWith("PRAGMA")) {
                continue;
            }

            // 检测 BEGIN 关键字（进入 BEGIN...END 块）
            if (trimmedLine.toUpperCase().equals("BEGIN")) {
                insideBeginEnd = true;
                current.append(line).append("\n");
                continue;
            }

            // 检测 END; 关键字（结束 BEGIN...END 块）
            if (insideBeginEnd && trimmedLine.toUpperCase().startsWith("END")) {
                insideBeginEnd = false;
                current.append(line);
                String stmt = current.toString().trim();
                // 去除末尾分号
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                }
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
                continue;
            }

            // 在 BEGIN...END 块内，保留所有内容（包括分号）
            if (insideBeginEnd) {
                current.append(line).append("\n");
                continue;
            }

            // 普通语句：按行尾分号分割
            if (trimmedLine.endsWith(";")) {
                current.append(line);
                String stmt = current.toString().trim();
                if (stmt.endsWith(";")) {
                    stmt = stmt.substring(0, stmt.length() - 1).trim();
                }
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            } else {
                current.append(line).append("\n");
            }
        }

        // 处理最后一条语句（如果没有分号结尾）
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            if (remaining.endsWith(";")) {
                remaining = remaining.substring(0, remaining.length() - 1).trim();
            }
            if (!remaining.isEmpty()) {
                statements.add(remaining);
            }
        }

        return statements;
    }
}
