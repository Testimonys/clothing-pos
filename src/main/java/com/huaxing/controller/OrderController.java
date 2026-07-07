package com.huaxing.controller;

import com.huaxing.dto.CreateOrderRequest;
import com.huaxing.entity.Order;
import com.huaxing.entity.OrderItem;
import com.huaxing.entity.ProductSku;
import com.huaxing.entity.StockRecord;
import com.huaxing.entity.SysUser;
import com.huaxing.enums.PayMethod;
import com.huaxing.enums.StockType;
import com.huaxing.repository.OrderItemRepository;
import com.huaxing.repository.OrderRepository;
import com.huaxing.repository.ProductSkuRepository;
import com.huaxing.repository.StockRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final StockRecordRepository stockRecordRepository;

    @Autowired(required = false)
    private Object escPosPrinter;

    public OrderController(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           ProductSkuRepository productSkuRepository,
                           StockRecordRepository stockRecordRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productSkuRepository = productSkuRepository;
        this.stockRecordRepository = stockRecordRepository;
    }

    /**
     * POST /api/order
     * 创建订单（事务）：校验库存 → 生成单号 → 存订单 → 扣库存 → 写流水 → 存明细
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreateOrderRequest request,
                                    @AuthenticationPrincipal SysUser cashier) {
        // 校验请求参数
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "订单明细不能为空"));
        }

        // 校验支付方式
        PayMethod payMethod;
        try {
            payMethod = PayMethod.valueOf(request.getPayMethod().toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "无效的支付方式: " + request.getPayMethod()));
        }

        // 1. 校验所有 SKU 库存是否充足，并预加载 SKU 信息
        List<ProductSku> skuList = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest item : request.getItems()) {
            if (item.getSkuId() == null || item.getQty() == null || item.getQty() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "无效的订单项: skuId或qty不合法"));
            }

            ProductSku sku = productSkuRepository.findById(item.getSkuId()).orElse(null);
            if (sku == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "SKU不存在: " + item.getSkuId()));
            }

            int stockQty = sku.getStockQty() != null ? sku.getStockQty() : 0;
            if (stockQty < item.getQty()) {
                String skuName = (sku.getColor() != null ? sku.getColor() : "")
                        + (sku.getSize() != null ? " / " + sku.getSize() : "");
                return ResponseEntity.badRequest().body(Map.of("message",
                        "库存不足: " + skuName + " 当前库存 " + stockQty + ", 需要 " + item.getQty()));
            }

            skuList.add(sku);
        }

        // 2. 生成单号 POS + yyyyMMdd + 4位序号（synchronized 防并发重复单号）
        String orderNo;
        try {
            orderNo = generateOrderNo();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        // 3. 保存 Order
        Order order = Order.builder()
                .orderNo(orderNo)
                .totalAmount(request.getTotalAmount() != null ? request.getTotalAmount() : BigDecimal.ZERO)
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .payAmount(request.getPayAmount() != null ? request.getPayAmount() : BigDecimal.ZERO)
                .receiveAmount(request.getReceiveAmount() != null ? request.getReceiveAmount() : BigDecimal.ZERO)
                .changeAmount(request.getChangeAmount() != null ? request.getChangeAmount() : BigDecimal.ZERO)
                .payMethod(payMethod)
                .cashier(cashier)
                .build();
        order = orderRepository.save(order);

        // 4. 逐个扣库存 → 写 OUTBOUND 流水 → 保存 OrderItem
        Long operatorId = cashier != null ? cashier.getId() : null;
        List<Map<String, Object>> savedItems = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemRequest itemReq = request.getItems().get(i);
            ProductSku sku = skuList.get(i);

            // 冗余商品名称和 SKU 规格
            String productName = "";
            String skuSpec = "";
            if (sku.getProductId() != null) {
                productName = sku.getProductId().getName();
                skuSpec = (sku.getColor() != null ? sku.getColor() : "")
                        + (sku.getSize() != null ? " / " + sku.getSize() : "");
            }

            // 扣减库存
            int beforeQty = sku.getStockQty() != null ? sku.getStockQty() : 0;
            int afterQty = beforeQty - itemReq.getQty();
            sku.setStockQty(afterQty);
            productSkuRepository.save(sku);

            // 写 OUTBOUND 流水
            StockRecord record = StockRecord.builder()
                    .sku(sku)
                    .productName(productName)
                    .skuSpec(skuSpec)
                    .type(StockType.OUTBOUND)
                    .qty(itemReq.getQty())
                    .beforeQty(beforeQty)
                    .afterQty(afterQty)
                    .operatorId(operatorId)
                    .build();
            stockRecordRepository.save(record);

            // 保存 OrderItem（冗余 productName + skuSpec）
            OrderItem orderItem = OrderItem.builder()
                    .orderId(order)
                    .skuId(sku.getId())
                    .productName(productName)
                    .skuSpec(skuSpec)
                    .unitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO)
                    .qty(itemReq.getQty())
                    .discount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO)
                    .subTotal(itemReq.getSubTotal() != null ? itemReq.getSubTotal() : BigDecimal.ZERO)
                    .barcode(sku.getBarcode())
                    .build();
            orderItemRepository.save(orderItem);

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("skuId", sku.getId());
            itemMap.put("productName", productName);
            itemMap.put("qty", itemReq.getQty());
            savedItems.add(itemMap);
        }

        // 5. 触发打印（如 printReceipt=true 且打印机可用）
        if (request.isPrintReceipt() && escPosPrinter != null) {
            try {
                // 占位：Task14 实现打印后接入
                // escPosPrinter.printOrder(order);
            } catch (Exception e) {
                // 打印失败不影响订单创建
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("createTime", order.getCreateTime());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/order?page=0&size=20&startTime=&endTime=&payMethod=
     * 订单列表：分页 + 时间范围 + 支付方式筛选，按时间倒序
     */
    @GetMapping
    public ResponseEntity<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String payMethod) {

        LocalDateTime start = null;
        LocalDateTime end = null;
        try {
            if (startTime != null && !startTime.isEmpty()) {
                start = LocalDateTime.parse(startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                end = LocalDateTime.parse(endTime);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }

        PayMethod pm = null;
        if (payMethod != null && !payMethod.isEmpty()) {
            try {
                pm = PayMethod.valueOf(payMethod.toUpperCase());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(null);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orderPage = orderRepository.search(start, end, pm, pageable);
        Page<Map<String, Object>> dtoPage = orderPage.map(this::toOrderSummary);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * GET /api/order/{id}
     * 订单详情（含 items）
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    // 触发懒加载 items
                    if (order.getItems() != null) {
                        order.getItems().size();
                    }
                    return ResponseEntity.ok(toOrderDetail(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/order/{id}/print
     * 补打小票（当前为占位，Task14 实现打印后接入）
     */
    @PostMapping("/{id}/print")
    public ResponseEntity<?> print(@PathVariable Long id) {
        if (!orderRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (escPosPrinter == null) {
            return ResponseEntity.ok(Map.of("message", "打印服务暂未接入"));
        }

        // 占位：Task14 实现打印后接入
        // Order order = orderRepository.findById(id).orElse(null);
        // escPosPrinter.printOrder(order);

        return ResponseEntity.ok(Map.of("message", "补打指令已发送"));
    }

    /**
     * 生成订单单号：POS + yyyyMMdd + 4位序号
     * synchronized 防止并发请求产生重复单号
     */
    private synchronized String generateOrderNo() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long countToday = orderRepository.countOrdersToday(todayStart);
        int seq = (countToday != null) ? countToday.intValue() + 1 : 1;
        if (seq > 9999) {
            throw new IllegalStateException("今日订单序号已用完（最大9999）");
        }
        return "POS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", seq);
    }

    // ---- DTO mapping methods ----

    private Map<String, Object> toOrderSummary(Order order) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.getId());
        map.put("orderNo", order.getOrderNo());
        map.put("totalAmount", order.getTotalAmount());
        map.put("discount", order.getDiscount());
        map.put("payAmount", order.getPayAmount());
        map.put("receiveAmount", order.getReceiveAmount());
        map.put("changeAmount", order.getChangeAmount());
        map.put("payMethod", order.getPayMethod().name());
        if (order.getCashier() != null) {
            map.put("cashierName", order.getCashier().getDisplayName());
        }
        map.put("createTime", order.getCreateTime());
        return map;
    }

    private Map<String, Object> toOrderDetail(Order order) {
        Map<String, Object> map = toOrderSummary(order);
        if (order.getItems() != null) {
            List<Map<String, Object>> itemList = order.getItems().stream().map(item -> {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", item.getId());
                im.put("skuId", item.getSkuId());
                im.put("productName", item.getProductName());
                im.put("skuSpec", item.getSkuSpec());
                im.put("unitPrice", item.getUnitPrice());
                im.put("qty", item.getQty());
                im.put("discount", item.getDiscount());
                im.put("subTotal", item.getSubTotal());
                im.put("barcode", item.getBarcode());
                return im;
            }).toList();
            map.put("items", itemList);
        }
        return map;
    }
}
