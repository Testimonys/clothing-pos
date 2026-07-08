package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.StockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockRecordMapper extends BaseMapper<StockRecord> {

    /** 按 SKU ID 查询库存流水，时间降序 */
    @Select("SELECT * FROM stock_record WHERE sku_id = #{skuId} ORDER BY create_time DESC")
    List<StockRecord> findBySkuIdOrderByCreateTimeDesc(Long skuId);
}
