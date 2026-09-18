package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/** luohuai codeX generate: immutable audit entry for corrected product dealer ownership. */
@TableName("product_dealer_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDealerHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long oldDealerId;
    private Long newDealerId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
