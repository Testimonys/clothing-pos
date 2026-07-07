package com.huaxing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String imageUrl;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ProductSkuDTO> skus;
}
