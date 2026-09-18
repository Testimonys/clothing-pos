package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.ColorConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** luohuai codeX generate: color dictionary persistence with immutable code allocation. */
@Mapper
public interface ColorConfigMapper extends BaseMapper<ColorConfig> {
    @Select("SELECT MAX(code) FROM color_config WHERE code <> '00'")
    String findMaxCode();
}
