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

    /** 分类 ID 外键 */
    @TableField("category_id")
    private Long categoryId;

    /** 分类实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private Category category;

    private String name;

    @TableField("image_url")
    private String imageUrl;

    @TableField("cost_price")
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
