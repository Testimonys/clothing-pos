package com.huaxing.config;

import com.huaxing.controller.UploadController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** luohuai codeX generate: expose controlled upload errors in the message format expected by the frontend. */
@RestControllerAdvice(assignableTypes = UploadController.class)
public class UploadExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> uploadError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason() == null ? "上传失败" : error.getReason()));
    }
}
