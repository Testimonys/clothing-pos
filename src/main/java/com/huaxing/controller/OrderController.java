package com.huaxing.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huaxing.config.PageUtils;
import com.huaxing.dto.CreateOrderRequest;
import com.huaxing.entity.*;
import com.huaxing.enums.PayMethod;
import com.huaxing.enums.StockType;
import com.huaxing.mapper.*;
import com.huaxing.printer.EscPosPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductSkuMapper productSkuMapper;
    private final StockRecordMapper stockRecordMapper;
    private final ProductMapper productMapper;
    private final SysUserMapper sysUserMapper;

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired(required = false)
    private EscPosPrinter escPosPrinter;

    public OrderController(OrderMapper orderMapper,
                           OrderItemMapper orderItemMapper,
                           ProductSkuMapper productSkuMapper,
                           StockRecordMapper stockRecordMapper,
                           ProductMapper productMapper,
                           SysUserMapper sysUserMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productSkuMapper = productSkuMapper;
        this.stockRecordMapper = stockRecordMapper;
        this.productMapper = productMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * POST /api/order
     * 创建订单（事务）：校验库存 → 生成单号 → 存订单 → 扣库存 → 写流水 → 存明细
     */
    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreateOrderRequest request,
                                    @AuthenticationPrincipal SysUser cashier) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "订单明细不能为空"));
        }

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

            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
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

        // 2. 生成单号 POS + yyyyMMdd + 4位序号
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
                .cashierId(cashier != null ? cashier.getId() : null)
                .build();
        orderMapper.insert(order);

        // 4. 逐个扣库存 → 写 OUTBOUND 流水 → 保存 OrderItem
        Long operatorId = cashier != null ? cashier.getId() : null;
        List<Map<String, Object>> savedItems = new ArrayList<>();

        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemRequest itemReq = request.getItems().get(i);
            ProductSku sku = skuList.get(i);

            // 冗余商品名称和 SKU 规格
            String productName = "";
            String skuSpec = "";
            Product product = productMapper.selectById(sku.getProductId());
            if (product != null) {
                productName = product.getName();
                skuSpec = (sku.getColor() != null ? sku.getColor() : "")
                        + (sku.getSize() != null ? " / " + sku.getSize() : "");
            }

            // 扣减库存
            int beforeQty = sku.getStockQty() != null ? sku.getStockQty() : 0;
            int afterQty = beforeQty - itemReq.getQty();
            sku.setStockQty(afterQty);
            productSkuMapper.updateById(sku);

            // 写 OUTBOUND 流水
            StockRecord record = StockRecord.builder()
                    .skuId(sku.getId())
                    .productName(productName)
                    .skuSpec(skuSpec)
                    .type(StockType.OUTBOUND)
                    .qty(itemReq.getQty())
                    .beforeQty(beforeQty)
                    .afterQty(afterQty)
                    .operatorId(operatorId)
                    .build();
            stockRecordMapper.insert(record);

            // 保存 OrderItem（冗余 productName + skuSpec）
            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .skuId(sku.getId())
                    .productName(productName)
                    .skuSpec(skuSpec)
                    .unitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO)
                    .qty(itemReq.getQty())
                    .discount(itemReq.getDiscount() != null ? itemReq.getDiscount() : BigDecimal.ZERO)
                    .subTotal(itemReq.getSubTotal() != null ? itemReq.getSubTotal() : BigDecimal.ZERO)
                    .barcode(sku.getBarcode())
                    .build();
            orderItemMapper.insert(orderItem);

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("skuId", sku.getId());
            itemMap.put("productName", productName);
            itemMap.put("qty", itemReq.getQty());
            savedItems.add(itemMap);
        }

        // 5. 触发打印（如 printReceipt=true 且打印机可用）
        if (request.isPrintReceipt() && escPosPrinter != null) {
            try {
                // 加载订单明细用于打印
                List<OrderItem> items = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
                escPosPrinter.printOrder(order, items);
            } catch (Exception e) {
                log.warn("打印小票失败: {}", e.getMessage());
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
    public ResponseEntity<Map<String, Object>> list(
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

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(Order::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(Order::getCreateTime, end);
        }
        if (pm != null) {
            wrapper.eq(Order::getPayMethod, pm);
        }
        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> mpPage = new Page<>(page + 1, size);
        IPage<Order> orderPage = orderMapper.selectPage(mpPage, wrapper);
        return ResponseEntity.ok(PageUtils.convert(orderPage));
    }

    /**
     * GET /api/order/{id}
     * 订单详情（含 items）
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        // 查询订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        order.setItems(items);
        // 查询收银员
        if (order.getCashierId() != null) {
            order.setCashier(sysUserMapper.selectById(order.getCashierId()));
        }
        return ResponseEntity.ok(toOrderDetail(order));
    }

    /**
     * POST /api/order/{id}/print
     * 补打小票
     */
    @PostMapping("/{id}/print")
    public ResponseEntity<?> print(@PathVariable Long id) {
        if (orderMapper.selectById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        if (escPosPrinter == null) {
            return ResponseEntity.ok(Map.of("message", "打印服务未启用（app.printer.enabled=false）"));
        }

        Order order = orderMapper.selectById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        // 查询订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));

        escPosPrinter.printOrder(order, items);
        return ResponseEntity.ok(Map.of("message", "补打指令已发送"));
    }

    /**
     * 生成订单单号：POS + yyyyMMdd + 4位序号
     * synchronized 防止并发请求产生重复单号
     */
    private synchronized String generateOrderNo() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long countToday = orderMapper.countOrdersToday(todayStart);
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
