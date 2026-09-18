package com.huaxing.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** luohuai codeX generate: return actionable catalog errors without changing other modules' response contracts. */
@RestControllerAdvice(assignableTypes = {ProductController.class, ProductImportController.class})
public class ProductApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ProductApiExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> invalid(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason() == null ? "商品请求无效" : error.getReason()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException error) {
        log.warn("商品资料保存违反数据库约束", error);
        return ResponseEntity.status(409).body(Map.of("message", "货号或条码重复，或分类/规格仍有关联记录；本次保存已撤销，请刷新检查"));
    }
}
