package com.huaxing.controller;

import com.huaxing.entity.SizeConfig;
import com.huaxing.service.SpecificationConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** luohuai codeX  modify: manage stable size codes and disable used values instead of deleting barcode identity. */
@RestController
public class SizeConfigController {
    private final SpecificationConfigService service;

    public SizeConfigController(SpecificationConfigService service) { this.service = service; }

    @GetMapping("/api/sizes")
    // luohuai codeX  modify: return disabled values for historical display; clients disable them for new selection.
    public ResponseEntity<List<SizeConfig>> list() { return ResponseEntity.ok(service.listSizes(false)); }

    @GetMapping("/api/setting/sizes")
    public ResponseEntity<List<SizeConfig>> listForSetting() { return ResponseEntity.ok(service.listSizes(false)); }

    @PostMapping("/api/setting/sizes")
    public ResponseEntity<SizeConfig> create(@RequestBody SizeConfig dto) { return ResponseEntity.ok(service.createSize(dto)); }

    @PutMapping("/api/setting/sizes/{id}")
    public ResponseEntity<SizeConfig> update(@PathVariable Long id, @RequestBody SizeConfig dto) { return ResponseEntity.ok(service.updateSize(id, dto)); }

    @DeleteMapping("/api/setting/sizes/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.deleteSize(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
