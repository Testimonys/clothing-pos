package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@TableName("product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** luohuai codeX  modify: allow the complete catalog form to clear an optional category. */
    @TableField(value = "category_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long categoryId;

    /** 分类实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private Category category;

    private String name;

    /** luohuai codeX  modify: retain the store's article number separately from database IDs and SKU barcodes. */
    @TableField(value = "product_code", updateStrategy = FieldStrategy.ALWAYS)
    private String productCode;

    /** luohuai codeX  modify: display the store's selling unit without duplicating price fields. */
    private String unit;

    // luohuai codeX  modify: removing a product image must persist the cleared optional value.
    @TableField(value = "image_url", updateStrategy = FieldStrategy.ALWAYS)
    private String imageUrl;

    // luohuai codeX  modify: an unknown cost stays null instead of silently becoming a plausible zero.
    @TableField(value = "cost_price", insertStrategy = FieldStrategy.ALWAYS, updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal costPrice;

    @TableField("sell_price")
    private BigDecimal sellPrice;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** SKU 列表（非数据库字段，手动填充） */
    @TableField(exist = false)
    private List<ProductSku> skus;
}
