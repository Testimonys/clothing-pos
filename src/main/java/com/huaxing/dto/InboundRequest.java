package com.huaxing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InboundRequest {

    private List<InboundItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InboundItem {
        private Long skuId;
        private Integer qty;
    }
}
