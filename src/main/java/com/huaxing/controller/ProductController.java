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
import com.huaxing.service.ProductCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CategoryMapper categoryMapper;
    // luohuai codeX  modify: share validation and atomic catalog writes across existing endpoints.
    private final ProductCatalogService catalogService;

    /** 当日条码序号缓存：新增 SKU 时连续生成，避免未保存前序号重复 */
    private final Map<String, Integer> barcodeSeqCache = new ConcurrentHashMap<>();

    public ProductController(ProductMapper productMapper,
                             ProductSkuMapper productSkuMapper,
                             CategoryMapper categoryMapper, ProductCatalogService catalogService) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.categoryMapper = categoryMapper;
        this.catalogService = catalogService;
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
        // luohuai codeX  modify: match name, article number or barcode without duplicating paged products.
        if (keyword != null && !keyword.trim().isEmpty()) {
            String term = keyword.trim();
            wrapper.and(w -> w.like(Product::getName, term).or().like(Product::getProductCode, term)
                    .or().apply("EXISTS (SELECT 1 FROM product_sku search_sku WHERE search_sku.product_id = product.id AND search_sku.barcode LIKE {0})", "%" + term + "%"));
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime).orderByDesc(Product::getId);

        Page<Product> mpPage = new Page<>(page + 1, size);
        IPage<Product> productPage = productMapper.selectPage(mpPage, wrapper);

        // 转换为 DTO（填充 categoryName），并批量查询 SKU 填充规格标签，避免 N+1 查询
        List<Product> records = productPage.getRecords();
        List<ProductDTO> content = new ArrayList<>();
        if (records != null && !records.isEmpty()) {
            // 批量查询本页涉及的商品，一次性取出 SKU
            Map<Long, List<ProductSku>> skusByProduct = productSkuMapper.selectList(
                    new LambdaQueryWrapper<ProductSku>().in(ProductSku::getProductId,
                            records.stream().map(Product::getId).collect(Collectors.toList())))
                    .stream()
                    .collect(Collectors.groupingBy(ProductSku::getProductId));

            for (Product product : records) {
                ProductDTO dto = toDTO(product);
                dto.setSkus(skusByProduct.getOrDefault(product.getId(), Collections.emptyList())
                        .stream().map(this::toSkuDTO).collect(Collectors.toList()));
                content.add(dto);
            }
        }

        return ResponseEntity.ok(PageUtils.convertWithRecords(productPage, content));
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
        // luohuai codeX  modify: validate all color/size combinations before creating the product.
        return ResponseEntity.ok(toDetailDTO(catalogService.create(dto)));
    }

    /**
     * PUT /api/product/{id}
     * 更新商品基本信息
     */
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        // luohuai codeX  modify: validate article numbers and prices while preserving the original metadata endpoint.
        return ResponseEntity.ok(toDTO(catalogService.update(id, dto, false)));
    }

    /** luohuai codeX generate: save the complete product and SKU form atomically, without overwriting existing stock. */
    @PutMapping("/{id}/catalog")
    public ResponseEntity<?> updateCatalog(@PathVariable Long id, @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(toDetailDTO(catalogService.update(id, dto, true)));
    }

    /**
     * DELETE /api/product/{id}
     * 删除商品及关联SKU（先删SKU，再删商品）
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        // luohuai codeX  modify: deleting a product must not bypass the SKU stock/history protections.
        catalogService.deleteProduct(id);
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
        // luohuai codeX  modify: reject missing or duplicate color/size combinations.
        return ResponseEntity.ok(toSkuDTO(catalogService.addSku(productId, dto)));
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
        // luohuai codeX  modify: update only SKU metadata, never the stale stock value from an editing form.
        return ResponseEntity.ok(toSkuDTO(catalogService.updateSku(productId, skuId, dto)));
    }

    /**
     * DELETE /api/product/{productId}/sku/{skuId}
     * 删除SKU
     */
    @DeleteMapping("/{productId}/sku/{skuId}")
    @Transactional
    public ResponseEntity<?> deleteSku(@PathVariable Long productId, @PathVariable Long skuId) {
        // luohuai codeX  modify: standalone deletions obey the same catalog rules as the atomic editor.
        catalogService.deleteSku(productId, skuId);
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
        // luohuai codeX  modify: barcode regeneration must not write back a stock snapshot.
        ProductSkuDTO barcodeChange = ProductSkuDTO.builder().color(sku.getColor()).size(sku.getSize()).barcode(barcode).build();
        catalogService.updateSku(productId, skuId, barcodeChange);

        return ResponseEntity.ok(Map.of("barcode", barcode));
    }

    /**
     * POST /api/product/barcode/generate
     * 生成下一个可用条码（HUAXING + yyyyMMdd + 3位序号），供新增 SKU 时自动填充。
     * 结合数据库最大序号与内存缓存，连续生成不重复（保存前未入库的序号也会递增）。
     */
    @PostMapping("/barcode/generate")
    public synchronized ResponseEntity<Map<String, Object>> generateNextBarcode() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "HUAXING" + today;

        int dbSeq = 0;
        String maxBarcode = productSkuMapper.findMaxBarcodeByPrefix(prefix);
        if (maxBarcode != null && maxBarcode.length() >= prefix.length() + 3) {
            try {
                dbSeq = Integer.parseInt(maxBarcode.substring(prefix.length()));
            } catch (NumberFormatException ignored) {
                // 忽略非法序号
            }
        }
        int seq = Math.max(dbSeq, barcodeSeqCache.getOrDefault(prefix, 0)) + 1;
        if (seq > 999) {
            return ResponseEntity.badRequest().body(Map.of("message", "今日条码序号已用完（最大999）"));
        }
        barcodeSeqCache.put(prefix, seq);
        return ResponseEntity.ok(Map.of("barcode", prefix + String.format("%03d", seq)));
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
        // luohuai codeX  modify: share reservations with form-generated barcodes so both entry points stay distinct.
        seq = Math.max(seq, barcodeSeqCache.getOrDefault(prefix, 0) + 1);
        if (seq > 999) {
            throw new IllegalStateException("今日条码序号已用完（最大999）");
        }
        barcodeSeqCache.put(prefix, seq);
        return prefix + String.format("%03d", seq);
    }

    // ---- DTO mapping methods ----

    private ProductDTO toDTO(Product product) {
        ProductDTO.ProductDTOBuilder builder = ProductDTO.builder()
                .id(product.getId())
                // luohuai codeX  modify: return article number and unit to existing product clients.
                .productCode(product.getProductCode())
                .unit(product.getUnit())
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
