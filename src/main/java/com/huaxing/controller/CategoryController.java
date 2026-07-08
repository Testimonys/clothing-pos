package com.huaxing.controller;

import com.huaxing.dto.CategoryDTO;
import com.huaxing.entity.Category;
import com.huaxing.mapper.CategoryMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setting/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> list() {
        List<CategoryDTO> list = categoryMapper.selectList(null).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@RequestBody CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(dto.getParentId());
        category.setSortOrder(dto.getSortOrder());
        categoryMapper.insert(category);
        return ResponseEntity.ok(toDTO(category));
    }

    @PutMapping("/{id}")
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
