package com.huaxing.controller;

import com.huaxing.dto.CategoryDTO;
import com.huaxing.entity.Category;
import com.huaxing.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/setting/categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> list() {
        List<CategoryDTO> list = categoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> create(@RequestBody CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        if (dto.getParentId() != null) {
            category.setParent(categoryRepository.findById(dto.getParentId()).orElse(null));
        }
        category.setSortOrder(dto.getSortOrder());
        category = categoryRepository.save(category);
        return ResponseEntity.ok(toDTO(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CategoryDTO dto) {
        return categoryRepository.findById(id)
                .map(category -> {
                    category.setName(dto.getName());
                    if (dto.getParentId() != null) {
                        category.setParent(categoryRepository.findById(dto.getParentId()).orElse(null));
                    } else {
                        category.setParent(null);
                    }
                    category.setSortOrder(dto.getSortOrder());
                    categoryRepository.save(category);
                    return ResponseEntity.ok(toDTO(category));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private CategoryDTO toDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .sortOrder(category.getSortOrder())
                .createTime(category.getCreateTime())
                .build();
    }
}
