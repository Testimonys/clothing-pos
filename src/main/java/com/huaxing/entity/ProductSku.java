package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@TableName("product_sku")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSku {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID 外键 */
    @TableField("product_id")
    private Long productId;

    /** 商品实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private Product product;

    private String color;

    @TableField("`size`")
    private String size;

    @TableField("barcode")
    private String barcode;

    @TableField("stock_qty")
    private Integer stockQty;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Long version;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
