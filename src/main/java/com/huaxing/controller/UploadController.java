package com.huaxing.controller;

import com.huaxing.service.CosStorageService;
import com.huaxing.service.CosStorageService.UploadResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** luohuai codeX  modify: replace local file writes with shared COS storage while keeping the image URL contract. */
@RestController
@RequestMapping("/api/upload")
public class UploadController {
    private final CosStorageService storage;

    public UploadController(CosStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(value = "/image", consumes = "multipart/form-data")
    public ResponseEntity<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storage.uploadImage(file));
    }

    /** luohuai codeX generate: general attachments use the same authenticated COS upload service. */
    @PostMapping(value = "/file", consumes = "multipart/form-data")
    public ResponseEntity<UploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(storage.uploadFile(file));
    }
}
