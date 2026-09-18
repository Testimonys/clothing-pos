package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/** luohuai codeX generate: dealer master data supplies immutable three-digit barcode codes. */
@TableName("dealer")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dealer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    @TableField("contact_name")
    private String contactName;
    private String phone;
    private String address;
    private String remark;
    private Boolean enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
