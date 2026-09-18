package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/** luohuai codeX generate: global selectable color dictionary with stable barcode codes. */
@TableName("color_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColorConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Boolean enabled;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
