package com.huaxing.repository;

import com.huaxing.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    /**
     * 查找以指定前缀开头的最大条码，用于条码自增序号生成
     */
    @Query("SELECT MAX(s.barcode) FROM ProductSku s WHERE s.barcode LIKE :prefix%")
    String findMaxBarcodeByPrefix(@Param("prefix") String prefix);
}
