package com.huaxing.repository;

import com.huaxing.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);
}
