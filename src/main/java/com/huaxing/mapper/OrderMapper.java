package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /** 今日订单计数（用于生成单号序号） */
    @Select("SELECT COUNT(*) FROM sys_order WHERE create_time >= #{todayStart}")
    Long countOrdersToday(LocalDateTime todayStart);
}
