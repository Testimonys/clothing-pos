package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 动态条件查询（keyword + categoryId）在 Controller 中通过 LambdaQueryWrapper 实现
}
