package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;

@TableName("order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单 ID 外键 */
    @TableField("order_id")
    private Long orderId;

    /** 订单实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private Order order;

    /** SKU ID（仅存 ID，不关联实体） */
    @TableField("sku_id")
    private Long skuId;

    @TableField("product_name")
    private String productName;

    @TableField("sku_spec")
    private String skuSpec;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    private Integer qty;

    private BigDecimal discount;

    @TableField("sub_total")
    private BigDecimal subTotal;

    private String barcode;
}
