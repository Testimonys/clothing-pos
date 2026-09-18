package com.huaxing.controller;

import com.huaxing.dto.ProductImportPreview;
import com.huaxing.service.ProductImportPreviewService;
import com.huaxing.service.ProductImportService;
import com.huaxing.dto.ProductImportResult;
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
    private final ProductImportService importService;

    public ProductImportController(ProductImportPreviewService service, ProductImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    public ProductImportPreview preview(@RequestParam("file") MultipartFile file) {
        ProductImportPreview preview = service.preview(file);
        log.info("商品导入预览完成：rows={}, ready={}, needsSpec={}, conflicts={}, invalid={}", preview.getTotalRows(),
                preview.getReadyRows(), preview.getNeedsSpecRows(), preview.getConflictRows(), preview.getInvalidRows());
        return preview;
    }

    /** luohuai codeX generate: re-parse and re-check the file inside the write request to prevent stale preview imports. */
    @PostMapping(value = "/commit", consumes = "multipart/form-data")
    public ProductImportResult commit(@RequestParam("file") MultipartFile file) {
        ProductImportResult result = importService.commit(file);
        log.info("商品导入完成：dealer={}, products={}, skus={}", result.dealerName(), result.productsCreated(), result.skusCreated());
        return result;
    }
}
