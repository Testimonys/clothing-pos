package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huaxing.enums.StockType;
import lombok.*;

import java.time.LocalDateTime;

@TableName("stock_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SKU ID 外键 */
    @TableField("sku_id")
    private Long skuId;

    /** SKU 实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private ProductSku sku;

    @TableField("product_name")
    private String productName;

    @TableField("sku_spec")
    private String skuSpec;

    private StockType type;

    private Integer qty;

    @TableField("before_qty")
    private Integer beforeQty;

    @TableField("after_qty")
    private Integer afterQty;

    @TableField("operator_id")
    private Long operatorId;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
