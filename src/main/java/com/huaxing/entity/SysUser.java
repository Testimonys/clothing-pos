package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huaxing.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;

@TableName("sys_user")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    @TableField("display_name")
    private String displayName;

    private UserRole role;

    @Builder.Default
    private Boolean enabled = true;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
