package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.SizeConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SizeConfigMapper extends BaseMapper<SizeConfig> {
    /** luohuai codeX generate: allocate normal codes without consuming the reserved 00 value. */
    @Select("SELECT MAX(code) FROM size_config WHERE code <> '00'")
    String findMaxCode();
}
