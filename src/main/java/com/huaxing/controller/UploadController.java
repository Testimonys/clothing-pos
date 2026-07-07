package com.huaxing.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "文件为空"));
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/gif"))) {
            return ResponseEntity.badRequest().body(Map.of("message", "仅支持 jpg/png/gif 格式"));
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = getExtension(originalFilename);
            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("gif")) {
                return ResponseEntity.badRequest().body(Map.of("message", "仅支持 jpg/png/gif 格式"));
            }
        }

        try {
            // Generate unique filename: yyyyMMdd_HHmmss_random6.ext
            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String randomPart = UUID.randomUUID().toString().substring(0, 6);
            String ext = getExtension(originalFilename);
            String filename = datePart + "_" + randomPart + "." + ext;

            // Ensure upload directory exists
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save file
            File dest = uploadDir.resolve(filename).toFile();
            file.transferTo(dest);

            return ResponseEntity.ok(Map.of("url", "/uploads/" + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "文件上传失败: " + e.getMessage()));
        }
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "jpg";
        }
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex == -1) {
            return "jpg";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
