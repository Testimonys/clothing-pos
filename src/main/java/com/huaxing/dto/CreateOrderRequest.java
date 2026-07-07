package com.huaxing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    private List<OrderItemRequest> items;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal payAmount;
    private BigDecimal receiveAmount;
    private BigDecimal changeAmount;
    private String payMethod;
    private boolean printReceipt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long skuId;
        private Integer qty;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal subTotal;
    }
}
