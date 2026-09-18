package com.huaxing.controller;

import com.huaxing.entity.ColorConfig;
import com.huaxing.service.SpecificationConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** luohuai codeX generate: expose selectable colors to all users and management actions to administrators. */
@RestController
public class ColorConfigController {
    private final SpecificationConfigService service;

    public ColorConfigController(SpecificationConfigService service) { this.service = service; }

    @GetMapping("/api/colors")
    // luohuai codeX  modify: return disabled values for historical display; clients disable them for new selection.
    public ResponseEntity<List<ColorConfig>> list() { return ResponseEntity.ok(service.listColors(false)); }

    @GetMapping("/api/setting/colors")
    public ResponseEntity<List<ColorConfig>> listForSetting() { return ResponseEntity.ok(service.listColors(false)); }

    @PostMapping("/api/setting/colors")
    public ResponseEntity<ColorConfig> create(@RequestBody ColorConfig dto) { return ResponseEntity.ok(service.createColor(dto)); }

    @PutMapping("/api/setting/colors/{id}")
    public ResponseEntity<ColorConfig> update(@PathVariable Long id, @RequestBody ColorConfig dto) { return ResponseEntity.ok(service.updateColor(id, dto)); }

    @DeleteMapping("/api/setting/colors/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        service.deleteColor(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
