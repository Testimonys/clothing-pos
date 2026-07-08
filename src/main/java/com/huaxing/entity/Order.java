package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huaxing.enums.PayMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@TableName("sys_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    private BigDecimal discount;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("receive_amount")
    private BigDecimal receiveAmount;

    @TableField("change_amount")
    private BigDecimal changeAmount;

    @TableField("pay_method")
    private PayMethod payMethod;

    /** 收银员 ID 外键 */
    @TableField("cashier_id")
    private Long cashierId;

    /** 收银员实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private SysUser cashier;

    /** 会员 ID（预留） */
    @TableField("member_id")
    private Long memberId;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 订单明细列表（非数据库字段，手动填充） */
    @TableField(exist = false)
    private List<OrderItem> items;
}
