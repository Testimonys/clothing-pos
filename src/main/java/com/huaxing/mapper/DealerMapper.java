package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.Dealer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** luohuai codeX generate: dealer persistence and next-code lookup. */
@Mapper
public interface DealerMapper extends BaseMapper<Dealer> {
    @Select("SELECT MAX(code) FROM dealer")
    String findMaxCode();
}
