package com.huaxing.service;

import com.huaxing.config.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** luohuai codeX generate: one stream-based COS upload path for images and files; no local storage fallback. */
@Service
public class CosStorageService {
    private static final Logger log = LoggerFactory.getLogger(CosStorageService.class);
    private static final long MB = 1024 * 1024;
    private static final Map<String, String> IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif");
    private final CosProperties properties;
    private final ObjectProvider<COSClient> clients;

    public CosStorageService(CosProperties properties, ObjectProvider<COSClient> clients) {
        this.properties = properties;
        this.clients = clients;
    }

    public UploadResult uploadImage(MultipartFile file) { return upload(file, true); }
    public UploadResult uploadFile(MultipartFile file) { return upload(file, false); }

    private UploadResult upload(MultipartFile file, boolean image) {
        if (file == null || file.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "文件为空");
        if (file.getSize() > (image ? 5 : 10) * MB) {
            throw error(HttpStatus.PAYLOAD_TOO_LARGE, image ? "图片大小不能超过5MB" : "文件大小不能超过10MB");
        }
        String filename = filename(file.getOriginalFilename());
        String extension = extension(filename);
        String contentType = IMAGE_TYPES.get(extension);
        if (image && (contentType == null || !contentType.equalsIgnoreCase(file.getContentType()))) {
            throw error(HttpStatus.BAD_REQUEST, "仅支持扩展名与类型一致的 jpg/png/gif 图片");
        }
        String baseUrl = validateConfiguration();
        String prefix = text(properties.getKeyPrefix()).replaceAll("^/+|/+$", "");
        if (!prefix.isEmpty() && !prefix.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*")) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "COS key-prefix 仅支持字母、数字、下划线、短横线和目录分隔符");
        }
        String key = (prefix.isEmpty() ? "" : prefix + "/") + (image ? "images/" : "files/")
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/"
                + UUID.randomUUID().toString().replace("-", "") + (extension.isEmpty() ? "" : "." + extension);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(image ? contentType : "application/octet-stream");
        if (!image) {
            metadata.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString());
        }
        try (InputStream stream = file.getInputStream()) {
            clients.getObject().putObject(new PutObjectRequest(text(properties.getBucketName()), key, stream, metadata));
            return new UploadResult(baseUrl + "/" + key, key, filename, file.getSize());
        } catch (CosClientException e) {
            // luohuai codeX generate: return controlled errors, never SDK request details or credentials.
            log.warn("COS上传失败：type={}, requestId={}", e.getClass().getSimpleName(),
                    e instanceof CosServiceException serviceError ? serviceError.getRequestId() : "unavailable");
            throw error(HttpStatus.BAD_GATEWAY, "COS上传失败，请检查服务端配置、存储桶权限及网络");
        } catch (IOException e) {
            log.warn("读取上传文件失败：type={}", e.getClass().getSimpleName());
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "读取上传文件失败，请重试");
        }
    }

    private String validateConfiguration() {
        if (placeholder(properties.getSecretId()) || placeholder(properties.getSecretKey())
                || placeholder(properties.getBucketName()) || placeholder(properties.getRegion())) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "COS尚未配置，请填写 secret-id、secret-key、bucket-name 和 region");
        }
        // luohuai codeX generate: reject malformed endpoint components before the SDK builds a remote request.
        if (!text(properties.getBucketName()).matches("[a-z0-9-]+-\\d+")
                || !text(properties.getRegion()).matches("[a-z0-9-]+")) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "COS bucket-name 或 region 格式不正确");
        }
        String url = text(properties.getPublicUrl());
        if (url.isEmpty()) url = "https://" + text(properties.getBucketName()) + ".cos." + text(properties.getRegion()) + ".myqcloud.com";
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "COS访问地址必须是有效的HTTPS地址，且不包含账号、查询参数或片段");
        }
        return url.replaceAll("/+$", "");
    }

    private static boolean placeholder(String value) {
        String normalized = text(value).toUpperCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.startsWith("YOUR_");
    }
    private static String text(String value) { return value == null ? "" : value.trim(); }
    private static String filename(String value) {
        String name = text(value).replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return name.isEmpty() ? "file" : name;
    }
    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.matches("[a-z0-9]{1,10}") ? ext : "";
    }
    private static ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }

    /** luohuai codeX generate: keep the existing url contract and expose the durable COS key for file consumers. */
    public record UploadResult(String url, String key, String filename, long size) { }
}
