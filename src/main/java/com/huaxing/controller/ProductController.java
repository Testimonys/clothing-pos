package com.huaxing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huaxing.config.PageUtils;
import com.huaxing.dto.ProductDTO;
import com.huaxing.dto.ProductSkuDTO;
import com.huaxing.entity.Category;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.mapper.CategoryMapper;
import com.huaxing.mapper.ProductMapper;
import com.huaxing.mapper.ProductSkuMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;

    public ProductController(ProductMapper productMapper,
                             ProductSkuMapper productSkuMapper,
                             CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.categoryMapper = categoryMapper;
    }

    /**
     * GET /api/product?keyword=&categoryId=&page=0&size=20
     * 分页搜索商品列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getName, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime);

        Page<Product> mpPage = new Page<>(page + 1, size);
        IPage<Product> productPage = productMapper.selectPage(mpPage, wrapper);
        return ResponseEntity.ok(PageUtils.convert(productPage));
    }

    /**
     * GET /api/product/{id}
     * 商品详情，包含SKU列表
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        // 查询关联的SKU列表
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        product.setSkus(skus);
        // 查询分类
        if (product.getCategoryId() != null) {
            product.setCategory(categoryMapper.selectById(product.getCategoryId()));
        }
        return ResponseEntity.ok(toDetailDTO(product));
    }

    /**
     * POST /api/product
     * 新建商品（含SKU列表），事务
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setImageUrl(dto.getImageUrl());
        product.setCostPrice(dto.getCostPrice());
        product.setSellPrice(dto.getSellPrice());
        product.setCategoryId(dto.getCategoryId());
        productMapper.insert(product);

        List<ProductSku> savedSkus = new ArrayList<>();
        if (dto.getSkus() != null) {
            for (ProductSkuDTO skuDTO : dto.getSkus()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product.getId());
                sku.setColor(skuDTO.getColor());
                sku.setSize(skuDTO.getSize());
                sku.setBarcode(skuDTO.getBarcode());
                sku.setStockQty(skuDTO.getStockQty() != null ? skuDTO.getStockQty() : 0);
                productSkuMapper.insert(sku);
                savedSkus.add(sku);
            }
        }
        product.setSkus(savedSkus);
        return ResponseEntity.ok(toDetailDTO(product));
    }

    /**
     * PUT /api/product/{id}
     * 更新商品基本信息
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        product.setName(dto.getName());
        product.setImageUrl(dto.getImageUrl());
        product.setCostPrice(dto.getCostPrice());
        product.setSellPrice(dto.getSellPrice());
        product.setCategoryId(dto.getCategoryId());
        productMapper.updateById(product);
        return ResponseEntity.ok(toDTO(product));
    }

    /**
     * DELETE /api/product/{id}
     * 删除商品及关联SKU（先删SKU，再删商品）
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (productMapper.selectById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        // 先删除关联的SKU
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, id));
        // 再删除商品
        productMapper.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    /**
     * GET /api/product/barcode/{code}
     * 扫码查询，返回商品和SKU信息
     */
    @GetMapping("/barcode/{code}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> queryByBarcode(@PathVariable String code) {
        return productSkuMapper.findByBarcode(code)
                .map(sku -> {
                    Long productId = sku.getProductId();
                    Product product = productMapper.selectById(productId);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("found", true);
                    result.put("skuId", sku.getId());
                    result.put("productId", productId);
                    result.put("productName", product != null ? product.getName() : "");
                    result.put("color", sku.getColor());
                    result.put("size", sku.getSize());
                    result.put("sellPrice", product != null ? product.getSellPrice() : null);
                    result.put("stockQty", sku.getStockQty());
                    result.put("skuSpec",
                            (sku.getColor() != null ? sku.getColor() : "") +
                                    (sku.getSize() != null ? " / " + sku.getSize() : ""));
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.ok(Map.of("found", false)));
    }

    /**
     * POST /api/product/{productId}/sku
     * 为商品添加SKU
     */
    @PostMapping("/{productId}/sku")
    @Transactional
    public ResponseEntity<?> addSku(@PathVariable Long productId, @RequestBody ProductSkuDTO dto) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        ProductSku sku = new ProductSku();
        sku.setProductId(productId);
        sku.setColor(dto.getColor());
        sku.setSize(dto.getSize());
        sku.setBarcode(dto.getBarcode());
        sku.setStockQty(dto.getStockQty() != null ? dto.getStockQty() : 0);
        productSkuMapper.insert(sku);
        return ResponseEntity.ok(toSkuDTO(sku));
    }

    /**
     * PUT /api/product/{productId}/sku/{skuId}
     * 更新SKU信息
     */
    @PutMapping("/{productId}/sku/{skuId}")
    @Transactional
    public ResponseEntity<?> updateSku(@PathVariable Long productId,
                                        @PathVariable Long skuId,
                                        @RequestBody ProductSkuDTO dto) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !productId.equals(sku.getProductId())) {
            return ResponseEntity.notFound().build();
        }
        sku.setColor(dto.getColor());
        sku.setSize(dto.getSize());
        sku.setBarcode(dto.getBarcode());
        sku.setStockQty(dto.getStockQty());
        productSkuMapper.updateById(sku);
        return ResponseEntity.ok(toSkuDTO(sku));
    }

    /**
     * DELETE /api/product/{productId}/sku/{skuId}
     * 删除SKU
     */
    @DeleteMapping("/{productId}/sku/{skuId}")
    @Transactional
    public ResponseEntity<?> deleteSku(@PathVariable Long productId, @PathVariable Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !productId.equals(sku.getProductId())) {
            return ResponseEntity.notFound().build();
        }
        productSkuMapper.deleteById(skuId);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    /**
     * POST /api/product/{productId}/sku/{skuId}/barcode
     * 生成条码 规则: HUAXING + yyyyMMdd + 3位序号
     */
    @PostMapping("/{productId}/sku/{skuId}/barcode")
    @Transactional
    public ResponseEntity<?> generateBarcode(@PathVariable Long productId, @PathVariable Long skuId) {
        ProductSku sku = productSkuMapper.selectById(skuId);
        if (sku == null || !productId.equals(sku.getProductId())) {
            return ResponseEntity.notFound().build();
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "HUAXING" + today;

        String barcode;
        try {
            barcode = doGenerateBarcode(prefix);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
        sku.setBarcode(barcode);
        productSkuMapper.updateById(sku);

        return ResponseEntity.ok(Map.of("barcode", barcode));
    }

    /**
     * 同步生成条码，避免并发重复。
     * 规则: prefix + 3位递增序号（001~999）
     */
    private synchronized String doGenerateBarcode(String prefix) {
        String maxBarcode = productSkuMapper.findMaxBarcodeByPrefix(prefix);
        int seq = 1;
        if (maxBarcode != null && maxBarcode.length() >= prefix.length() + 3) {
            String seqStr = maxBarcode.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                // ignore, use default seq = 1
            }
        }
        if (seq > 999) {
            throw new IllegalStateException("今日条码序号已用完（最大999）");
        }
        return prefix + String.format("%03d", seq);
    }

    // ---- DTO mapping methods ----

    private ProductDTO toDTO(Product product) {
        ProductDTO.ProductDTOBuilder builder = ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .imageUrl(product.getImageUrl())
                .costPrice(product.getCostPrice())
                .sellPrice(product.getSellPrice())
                .createTime(product.getCreateTime())
                .updateTime(product.getUpdateTime());

        if (product.getCategoryId() != null) {
            Category category = categoryMapper.selectById(product.getCategoryId());
            builder.categoryId(product.getCategoryId());
            builder.categoryName(category != null ? category.getName() : null);
        }

        return builder.build();
    }

    private ProductDTO toDetailDTO(Product product) {
        ProductDTO dto = toDTO(product);
        if (product.getSkus() != null) {
            dto.setSkus(product.getSkus().stream()
                    .map(this::toSkuDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private ProductSkuDTO toSkuDTO(ProductSku sku) {
        return ProductSkuDTO.builder()
                .id(sku.getId())
                .productId(sku.getProductId())
                .color(sku.getColor())
                .size(sku.getSize())
                .barcode(sku.getBarcode())
                .stockQty(sku.getStockQty())
                .createTime(sku.getCreateTime())
                .build();
    }
}
