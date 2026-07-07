package com.huaxing.repository;

import com.huaxing.entity.StockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRecordRepository extends JpaRepository<StockRecord, Long> {

    /**
     * 按SKU ID查询库存流水，按创建时间降序排列
     */
    @Query("SELECT r FROM StockRecord r WHERE r.sku.id = :skuId ORDER BY r.createTime DESC")
    List<StockRecord> findBySkuIdOrderByCreateTimeDesc(@Param("skuId") Long skuId);
}
