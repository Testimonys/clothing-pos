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
    // luohuai codeX  modify: carry article numbers as text and expose selling units through the existing API.
    private String productCode;
    // luohuai codeX generate: bind and display the product's single fixed dealer.
    private Long dealerId;
    private String dealerCode;
    private String dealerName;
    // luohuai codeX generate: explicit confirmation protects dealer changes after stock or sales history exists.
    private Boolean dealerChangeConfirmed;
    private String unit;
    private String imageUrl;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<ProductSkuDTO> skus;
}
