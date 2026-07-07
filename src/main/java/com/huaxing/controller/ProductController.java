package com.huaxing.controller;

import com.huaxing.dto.ProductDTO;
import com.huaxing.dto.ProductSkuDTO;
import com.huaxing.entity.Category;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.repository.CategoryRepository;
import com.huaxing.repository.ProductRepository;
import com.huaxing.repository.ProductSkuRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductSkuRepository productSkuRepository;
    private final CategoryRepository categoryRepository;

    public ProductController(ProductRepository productRepository,
                             ProductSkuRepository productSkuRepository,
                             CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productSkuRepository = productSkuRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * GET /api/product?keyword=&categoryId=&page=0&size=20
     * 分页搜索商品列表
     */
    @GetMapping
    public ResponseEntity<Page<ProductDTO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Product> productPage = productRepository.search(keyword, categoryId, pageable);
        Page<ProductDTO> dtoPage = productPage.map(this::toDTO);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * GET /api/product/{id}
     * 商品详情，包含SKU列表
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    product.getSkus().size(); // 触发懒加载
                    return ResponseEntity.ok(toDetailDTO(product));
                })
                .orElse(ResponseEntity.notFound().build());
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

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
            product.setCategory(category);
        }

        List<ProductSku> skus = new ArrayList<>();
        if (dto.getSkus() != null) {
            for (ProductSkuDTO skuDTO : dto.getSkus()) {
                ProductSku sku = new ProductSku();
                sku.setProductId(product);
                sku.setColor(skuDTO.getColor());
                sku.setSize(skuDTO.getSize());
                sku.setBarcode(skuDTO.getBarcode());
                sku.setStockQty(skuDTO.getStockQty() != null ? skuDTO.getStockQty() : 0);
                skus.add(sku);
            }
        }
        product.setSkus(skus);

        product = productRepository.save(product);
        return ResponseEntity.ok(toDetailDTO(product));
    }

    /**
     * PUT /api/product/{id}
     * 更新商品基本信息
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        return productRepository.findById(id)
                .map(product -> {
                    product.setName(dto.getName());
                    product.setImageUrl(dto.getImageUrl());
                    product.setCostPrice(dto.getCostPrice());
                    product.setSellPrice(dto.getSellPrice());
                    if (dto.getCategoryId() != null) {
                        Category category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
                        product.setCategory(category);
                    } else {
                        product.setCategory(null);
                    }
                    productRepository.save(product);
                    return ResponseEntity.ok(toDTO(product));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/product/{id}
     * 删除商品及关联SKU（级联删除）
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    /**
     * GET /api/product/barcode/{code}
     * 扫码查询，返回商品和SKU信息
     */
    @GetMapping("/barcode/{code}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> queryByBarcode(@PathVariable String code) {
        return productSkuRepository.findByBarcode(code)
                .map(sku -> {
                    Product product = sku.getProductId();
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("found", true);
                    result.put("skuId", sku.getId());
                    result.put("productId", product.getId());
                    result.put("productName", product.getName());
                    result.put("color", sku.getColor());
                    result.put("size", sku.getSize());
                    result.put("sellPrice", product.getSellPrice());
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
        return productRepository.findById(productId)
                .map(product -> {
                    ProductSku sku = new ProductSku();
                    sku.setProductId(product);
                    sku.setColor(dto.getColor());
                    sku.setSize(dto.getSize());
                    sku.setBarcode(dto.getBarcode());
                    sku.setStockQty(dto.getStockQty() != null ? dto.getStockQty() : 0);
                    sku = productSkuRepository.save(sku);
                    return ResponseEntity.ok(toSkuDTO(sku));
                })
                .orElse(ResponseEntity.notFound().build());
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
        return productSkuRepository.findById(skuId)
                .filter(sku -> sku.getProductId().getId().equals(productId))
                .map(sku -> {
                    sku.setColor(dto.getColor());
                    sku.setSize(dto.getSize());
                    sku.setBarcode(dto.getBarcode());
                    sku.setStockQty(dto.getStockQty());
                    sku = productSkuRepository.save(sku);
                    return ResponseEntity.ok(toSkuDTO(sku));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/product/{productId}/sku/{skuId}
     * 删除SKU
     */
    @DeleteMapping("/{productId}/sku/{skuId}")
    @Transactional
    public ResponseEntity<?> deleteSku(@PathVariable Long productId, @PathVariable Long skuId) {
        return productSkuRepository.findById(skuId)
                .filter(sku -> sku.getProductId().getId().equals(productId))
                .map(sku -> {
                    productSkuRepository.delete(sku);
                    return ResponseEntity.ok(Map.of("message", "ok"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/product/{productId}/sku/{skuId}/barcode
     * 生成条码 规则: HUAXING + yyyyMMdd + 3位序号
     */
    @PostMapping("/{productId}/sku/{skuId}/barcode")
    @Transactional
    public ResponseEntity<?> generateBarcode(@PathVariable Long productId, @PathVariable Long skuId) {
        return productSkuRepository.findById(skuId)
                .filter(sku -> sku.getProductId().getId().equals(productId))
                .map(sku -> {
                    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String prefix = "HUAXING" + today;

                    String maxBarcode = productSkuRepository.findMaxBarcodeByPrefix(prefix);
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
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "今日条码序号已用完（最大999）"));
                    }

                    String barcode = prefix + String.format("%03d", seq);
                    sku.setBarcode(barcode);
                    productSkuRepository.save(sku);

                    return ResponseEntity.ok(Map.of("barcode", barcode));
                })
                .orElse(ResponseEntity.notFound().build());
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

        if (product.getCategory() != null) {
            builder.categoryId(product.getCategory().getId());
            builder.categoryName(product.getCategory().getName());
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
                .productId(sku.getProductId() != null ? sku.getProductId().getId() : null)
                .color(sku.getColor())
                .size(sku.getSize())
                .barcode(sku.getBarcode())
                .stockQty(sku.getStockQty())
                .createTime(sku.getCreateTime())
                .build();
    }
}
