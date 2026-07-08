package com.huaxing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@TableName("category")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "name")
    private String name;

    /** 父分类 ID */
    @TableField("parent_id")
    private Long parentId;

    /** 父分类实体（非数据库字段，手动填充） */
    @TableField(exist = false)
    private Category parent;

    @TableField("sort_order")
    private Integer sortOrder;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
