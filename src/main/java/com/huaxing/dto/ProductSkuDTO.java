package com.huaxing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSkuDTO {

    private Long id;
    private Long productId;
    private String color;
    private String size;
    private String barcode;
    private Integer stockQty;
    private LocalDateTime createTime;
}
