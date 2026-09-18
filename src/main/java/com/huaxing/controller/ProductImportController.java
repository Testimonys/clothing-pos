package com.huaxing.controller;

import com.huaxing.dto.ProductImportPreview;
import com.huaxing.service.ProductImportPreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** luohuai codeX generate: authenticated preview endpoint performs no catalog or inventory writes. */
@RestController
@RequestMapping("/api/product/import")
public class ProductImportController {
    private static final Logger log = LoggerFactory.getLogger(ProductImportController.class);
    private final ProductImportPreviewService service;

    public ProductImportController(ProductImportPreviewService service) { this.service = service; }

    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    public ProductImportPreview preview(@RequestParam("file") MultipartFile file) {
        ProductImportPreview preview = service.preview(file);
        log.info("商品导入预览完成：rows={}, ready={}, needsSpec={}, conflicts={}, invalid={}", preview.getTotalRows(),
                preview.getReadyRows(), preview.getNeedsSpecRows(), preview.getConflictRows(), preview.getInvalidRows());
        return preview;
    }
}
