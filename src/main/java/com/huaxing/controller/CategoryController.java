package com.huaxing.controller;

import com.huaxing.dto.CategoryDTO;
import com.huaxing.entity.Category;
import com.huaxing.mapper.CategoryMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class CategoryController {

    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    /**
     * GET /api/categories
     * 分类只读列表，所有登录用户可用（商品页/收银台需要选分类）。
     * 不放在 /api/setting/** 下，避免被 BOSS 角色锁定。
     */
    @GetMapping("/api/categories")
    public ResponseEntity<List<CategoryDTO>> list() {
        List<CategoryDTO> list = categoryMapper.selectList(null).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/setting/categories
     * 分类管理列表（BOSS 专属，用于系统设置页），复用同一查询逻辑
     */
    @GetMapping("/api/setting/categories")
    public ResponseEntity<List<CategoryDTO>> listForSetting() {
        return list();
    }

    @PostMapping("/api/setting/categories")
    public ResponseEntity<CategoryDTO> create(@RequestBody CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder());
        categoryMapper.insert(category);
        return ResponseEntity.ok(toDTO(category));
    }

    @PutMapping("/api/setting/categories/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return ResponseEntity.notFound().build();
        }
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder());
        categoryMapper.updateById(category);
        return ResponseEntity.ok(toDTO(category));
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .createTime(category.getCreateTime())
                .build();
    }
}
