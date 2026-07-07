package com.huaxing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path backupPath;
    private final int keepDays;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public BackupService(@Value("${app.backup.path}") String backupPath,
                         @Value("${app.backup.keep-days}") int keepDays,
                         DataSource dataSource) {
        this.backupPath = Path.of(backupPath).toAbsolutePath().normalize();
        this.keepDays = keepDays;
        try {
            // 从 DataSource 连接中解析数据库连接参数
            try (Connection conn = dataSource.getConnection()) {
                String url = conn.getMetaData().getURL();
                String[] parsed = parseJdbcUrl(url);
                this.dbHost = parsed[0];
                this.dbPort = Integer.parseInt(parsed[1]);
                this.dbName = parsed[2];
                this.dbUser = conn.getMetaData().getUserName();
                this.dbPassword = (dataSource instanceof com.zaxxer.hikari.HikariDataSource)
                        ? ((com.zaxxer.hikari.HikariDataSource) dataSource).getPassword()
                        : "";
            }
        } catch (Exception e) {
            throw new RuntimeException("无法解析数据库连接信息", e);
        }
        // 确保备份目录存在
        try {
            Files.createDirectories(this.backupPath);
        } catch (IOException e) {
            throw new RuntimeException("无法创建备份目录: " + this.backupPath, e);
        }
        log.info("BackupService initialized, path: {}, keep-days: {}", this.backupPath, this.keepDays);
    }

    /**
     * 执行 mysqldump 生成备份文件
     * @return 备份文件信息: fileName, size, time
     */
    public Map<String, Object> backup() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String fileName = "backup_" + timestamp + ".sql";
        Path targetFile = backupPath.resolve(fileName);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "mysqldump",
                    "-h", dbHost,
                    "-P", String.valueOf(dbPort),
                    "-u", dbUser,
                    "-p" + dbPassword,
                    dbName
            );
            pb.redirectOutput(targetFile.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // 读取错误输出
                String error = new String(process.getErrorStream().readAllBytes());
                throw new IOException("mysqldump 执行失败, exitCode=" + exitCode + ", error: " + error);
            }

            // 执行清理
            cleanOldBackups();

            File file = targetFile.toFile();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileName", fileName);
            result.put("size", file.length());
            result.put("time", timestamp);
            log.info("备份完成: {}, size: {}", fileName, file.length());
            return result;

        } catch (IOException | InterruptedException e) {
            // 清理失败时产生的部分文件
            try {
                Files.deleteIfExists(targetFile);
            } catch (IOException ignored) {
            }
            throw new RuntimeException("备份失败: " + e.getMessage(), e);
        }
    }

    /**
     * 列出备份目录下所有 .sql 文件，按最后修改时间倒序
     */
    public List<Map<String, Object>> listBackups() {
        if (!Files.isDirectory(backupPath)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(backupPath)) {
            return stream
                    .filter(p -> p.toString().endsWith(".sql"))
                    .map(this::toBackupInfo)
                    .sorted((a, b) -> ((String) b.get("time")).compareTo((String) a.get("time")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("列出备份文件失败", e);
            return List.of();
        }
    }

    /**
     * 下载指定的备份文件（防路径穿越）
     */
    public Resource download(String name) {
        // 文件名合法性校验
        if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                || name.contains("..") || !name.endsWith(".sql")) {
            throw new IllegalArgumentException("非法文件名: " + name);
        }

        Path filePath = backupPath.resolve(name).normalize();
        // 路径穿越检查：确保解析后的路径仍在 backupPath 下
        if (!filePath.startsWith(backupPath)) {
            throw new SecurityException("路径穿越攻击已阻止: " + name);
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在: " + name);
        }

        return new FileSystemResource(file);
    }

    /**
     * 清理 keep-days 天前的备份文件
     */
    public void cleanOldBackups() {
        if (!Files.isDirectory(backupPath)) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        log.info("开始清理 {} 天前的备份文件, 截止时间: {}", keepDays, cutoff);

        try (Stream<Path> stream = Files.list(backupPath)) {
            List<Path> toDelete = stream
                    .filter(p -> p.toString().endsWith(".sql"))
                    .filter(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            LocalDateTime fileTime = LocalDateTime.ofInstant(
                                    attrs.lastModifiedTime().toInstant(),
                                    java.time.ZoneId.systemDefault());
                            return fileTime.isBefore(cutoff);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            for (Path p : toDelete) {
                try {
                    Files.delete(p);
                    log.info("已删除过期备份: {}", p.getFileName());
                } catch (IOException e) {
                    log.warn("删除备份文件失败: {}", p, e);
                }
            }
            log.info("清理完成, 共删除 {} 个文件", toDelete.size());
        } catch (IOException e) {
            log.error("清理备份文件失败", e);
        }
    }

    // ================= 辅助方法 =================

    private Map<String, Object> toBackupInfo(Path path) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("fileName", path.getFileName().toString());
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            info.put("size", attrs.size());
            info.put("time", LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(),
                    java.time.ZoneId.systemDefault())
                    .format(FORMATTER));
        } catch (IOException e) {
            info.put("size", 0L);
            info.put("time", "unknown");
        }
        return info;
    }

    /**
     * 解析 JDBC URL，返回 [host, port, dbName]
     * 支持格式: jdbc:mysql://host:port/dbname?params
     */
    private String[] parseJdbcUrl(String url) {
        try {
            // 去掉 jdbc: 前缀
            String cleanUrl = url.substring(5); // remove "jdbc:"
            URI uri = new URI(cleanUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String dbName = uri.getPath();
            if (dbName != null) {
                dbName = dbName.replaceFirst("^/", "");
                // 去掉可能的 query 参数
                int idx = dbName.indexOf('?');
                if (idx > 0) {
                    dbName = dbName.substring(0, idx);
                }
            }
            return new String[]{
                    host != null ? host : "localhost",
                    port > 0 ? String.valueOf(port) : "3306",
                    dbName != null ? dbName : ""
            };
        } catch (URISyntaxException e) {
            log.warn("解析 JDBC URL 失败: {}, 使用默认值", url);
            return new String[]{"localhost", "3306", "clothing_pos"};
        }
    }
}
