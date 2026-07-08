package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Select("SELECT * FROM product_sku WHERE barcode = #{barcode}")
    Optional<ProductSku> findByBarcode(String barcode);

    @Select("SELECT COUNT(*) > 0 FROM product_sku WHERE barcode = #{barcode}")
    boolean existsByBarcode(String barcode);

    /** 查找以指定前缀开头的最大条码，用于自增序号生成 */
    @Select("SELECT MAX(barcode) FROM product_sku WHERE barcode LIKE CONCAT(#{prefix}, '%')")
    String findMaxBarcodeByPrefix(String prefix);
}
