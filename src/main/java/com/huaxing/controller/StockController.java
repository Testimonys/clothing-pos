package com.huaxing.controller;

import com.huaxing.dto.InboundRequest;
import com.huaxing.entity.ProductSku;
import com.huaxing.entity.StockRecord;
import com.huaxing.entity.SysUser;
import com.huaxing.enums.StockType;
import com.huaxing.repository.ProductRepository;
import com.huaxing.repository.ProductSkuRepository;
import com.huaxing.repository.StockRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final ProductSkuRepository productSkuRepository;
    private final ProductRepository productRepository;
    private final StockRecordRepository stockRecordRepository;

    public StockController(ProductSkuRepository productSkuRepository,
                           ProductRepository productRepository,
                           StockRecordRepository stockRecordRepository) {
        this.productSkuRepository = productSkuRepository;
        this.productRepository = productRepository;
        this.stockRecordRepository = stockRecordRepository;
    }

    /**
     * GET /api/stock?page=0&size=20
     * 分页查询库存列表
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ProductSku> skuPage = productSkuRepository.findAll(pageable);
        Page<Map<String, Object>> dtoPage = skuPage.map(this::toStockDTO);
        return ResponseEntity.ok(dtoPage);
    }

    private Map<String, Object> toStockDTO(ProductSku sku) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("skuId", sku.getId());
        if (sku.getProductId() != null) {
            map.put("productId", sku.getProductId().getId());
            map.put("productName", sku.getProductId().getName());
        }
        map.put("color", sku.getColor());
        map.put("size", sku.getSize());
        map.put("barcode", sku.getBarcode());
        map.put("stockQty", sku.getStockQty());
        return map;
    }

    /**
     * GET /api/stock/{skuId}/flow
     * 查询指定SKU的库存流水
     */
    @GetMapping("/{skuId}/flow")
    public ResponseEntity<List<Map<String, Object>>> flow(@PathVariable Long skuId) {
        if (!productSkuRepository.existsById(skuId)) {
            return ResponseEntity.notFound().build();
        }
        List<StockRecord> records = stockRecordRepository.findBySkuIdOrderByCreateTimeDesc(skuId);
        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("type", r.getType().name());
            m.put("qty", r.getQty());
            m.put("beforeQty", r.getBeforeQty());
            m.put("afterQty", r.getAfterQty());
            m.put("productName", r.getProductName());
            m.put("skuSpec", r.getSkuSpec());
            m.put("operatorId", r.getOperatorId());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/stock/inbound
     * 入库操作（事务），更新库存数量并写入库存流水
     */
    @PostMapping("/inbound")
    @Transactional
    public ResponseEntity<?> inbound(@RequestBody InboundRequest request,
                                     @AuthenticationPrincipal SysUser operator) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "入库明细不能为空"));
        }

        Long operatorId = operator != null ? operator.getId() : null;
        List<Map<String, Object>> results = new ArrayList<>();

        for (InboundRequest.InboundItem item : request.getItems()) {
            if (item.getSkuId() == null || item.getQty() == null || item.getQty() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "无效的入库项: skuId或qty不合法"));
            }

            ProductSku sku = productSkuRepository.findById(item.getSkuId()).orElse(null);
            if (sku == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "SKU不存在: " + item.getSkuId()));
            }

            // 冗余商品名称和SKU规格
            String productName = "";
            String skuSpec = "";
            if (sku.getProductId() != null) {
                productName = sku.getProductId().getName();
                skuSpec = (sku.getColor() != null ? sku.getColor() : "")
                        + (sku.getSize() != null ? " / " + sku.getSize() : "");
            }

            // 更新库存数量
            int beforeQty = sku.getStockQty() != null ? sku.getStockQty() : 0;
            int afterQty = beforeQty + item.getQty();
            sku.setStockQty(afterQty);
            productSkuRepository.save(sku);

            // 写入库存流水记录
            StockRecord record = StockRecord.builder()
                    .sku(sku)
                    .productName(productName)
                    .skuSpec(skuSpec)
                    .type(StockType.INBOUND)
                    .qty(item.getQty())
                    .beforeQty(beforeQty)
                    .afterQty(afterQty)
                    .operatorId(operatorId)
                    .build();
            stockRecordRepository.save(record);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("skuId", sku.getId());
            result.put("beforeQty", beforeQty);
            result.put("afterQty", afterQty);
            result.put("qty", item.getQty());
            results.add(result);
        }

        return ResponseEntity.ok(Map.of("message", "入库成功", "items", results));
    }
}
