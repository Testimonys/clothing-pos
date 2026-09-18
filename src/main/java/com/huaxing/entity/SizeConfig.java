package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 尺码标签配置（全局字典，SKU 尺码下拉选择）
 */
@TableName("size_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 尺码名称，如 S / M / L / XL / 2XL */
    private String name;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
