package com.huaxing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huaxing.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /** luohuai codeX generate: metadata edits never overwrite live stock; invalidate concurrent stale stock reads. */
    @Update("UPDATE product_sku SET color = #{sku.color}, size = #{sku.size}, barcode = #{sku.barcode}, " +
            "version = COALESCE(version, 0) + 1 WHERE id = #{sku.id} AND product_id = #{sku.productId}")
    int updateMetadata(@Param("sku") ProductSku sku);

    /** luohuai codeX generate: keep historical SKU references intact when editing the specification list. */
    @Select("SELECT (SELECT COUNT(*) FROM stock_record WHERE sku_id = #{id}) + " +
            "(SELECT COUNT(*) FROM order_item WHERE sku_id = #{id})")
    long countHistory(Long id);

    @Select("SELECT * FROM product_sku WHERE barcode = #{barcode}")
    Optional<ProductSku> findByBarcode(String barcode);

    @Select("SELECT COUNT(*) > 0 FROM product_sku WHERE barcode = #{barcode}")
    boolean existsByBarcode(String barcode);

    /** 查找以指定前缀开头的最大条码，用于自增序号生成 */
    @Select("SELECT MAX(barcode) FROM product_sku WHERE barcode LIKE CONCAT(#{prefix}, '%')")
    String findMaxBarcodeByPrefix(String prefix);
}
