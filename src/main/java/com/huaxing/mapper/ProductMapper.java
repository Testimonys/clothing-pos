package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    /** luohuai codeX generate: serialize changes to one product's color/size combinations. */
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product lockById(Long id);
    // 动态条件查询（keyword + categoryId）在 Controller 中通过 LambdaQueryWrapper 实现
}
