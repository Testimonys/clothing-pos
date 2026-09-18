package com.huaxing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaxing.entity.SizeConfig;
import com.huaxing.mapper.SizeConfigMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class SizeConfigController {

    private final SizeConfigMapper sizeConfigMapper;

    public SizeConfigController(SizeConfigMapper sizeConfigMapper) {
        this.sizeConfigMapper = sizeConfigMapper;
    }

    /**
     * GET /api/sizes
     * 尺码标签只读列表（所有登录用户可用，SKU 下拉选择）
     */
    @GetMapping("/api/sizes")
    public ResponseEntity<List<SizeConfig>> list() {
        List<SizeConfig> list = sizeConfigMapper.selectList(
                new LambdaQueryWrapper<SizeConfig>().orderByAsc(SizeConfig::getSortOrder));
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/setting/sizes
     * 尺码标签管理列表（BOSS 专属，系统设置页）
     */
    @GetMapping("/api/setting/sizes")
    public ResponseEntity<List<SizeConfig>> listForSetting() {
        return list();
    }

    /**
     * POST /api/setting/sizes
     * 新增尺码标签
     */
    @PostMapping("/api/setting/sizes")
    public ResponseEntity<?> create(@RequestBody SizeConfig dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "尺码标签不能为空"));
        }
        SizeConfig size = SizeConfig.builder()
                .name(dto.getName().trim())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();
        sizeConfigMapper.insert(size);
        return ResponseEntity.ok(size);
    }

    /**
     * PUT /api/setting/sizes/{id}
     * 更新尺码标签
     */
    @PutMapping("/api/setting/sizes/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody SizeConfig dto) {
        SizeConfig size = sizeConfigMapper.selectById(id);
        if (size == null) {
            return ResponseEntity.notFound().build();
        }
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            size.setName(dto.getName().trim());
        }
        if (dto.getSortOrder() != null) {
            size.setSortOrder(dto.getSortOrder());
        }
        sizeConfigMapper.updateById(size);
        return ResponseEntity.ok(size);
    }

    /**
     * DELETE /api/setting/sizes/{id}
     * 删除尺码标签
     */
    @DeleteMapping("/api/setting/sizes/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (sizeConfigMapper.selectById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        sizeConfigMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }
}
