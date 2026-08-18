package com.huaxing.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 销售统计报表 Mapper（只读聚合查询，无对应实体表，故不继承 BaseMapper）。
 *
 * 注意：返回 Map 的聚合 SQL 不受 map-underscore-to-camel-case 影响，SQL 中已用驼峰别名，
 * 别名即 Map key。日期过滤统一用半开区间 {@code >= start AND < end}（含 end 当天）。
 */
@Mapper
public interface ReportMapper {

    /**
     * 订单级聚合：按日期分桶统计订单数/销售额/实收/让利。
     * 订单一行一条，天然不因明细 JOIN 而翻倍。
     */
    @Select("SELECT DATE_FORMAT(create_time, #{dateFormat}) AS `key`, " +
            "COUNT(*) AS orderCount, " +
            "COALESCE(SUM(total_amount), 0) AS salesAmount, " +
            "COALESCE(SUM(pay_amount), 0) AS payAmount, " +
            "COALESCE(SUM(discount), 0) AS discountAmount " +
            "FROM sys_order " +
            "WHERE create_time >= #{start} AND create_time < #{end} " +
            "GROUP BY DATE_FORMAT(create_time, #{dateFormat}) " +
            "ORDER BY `key`")
    List<Map<String, Object>> selectOrderAgg(LocalDateTime start, LocalDateTime end, String dateFormat);

    /**
     * 按日期分桶的成本：经 order_item → product_sku → product 取当前 cost_price。
     * 与 selectOrderAgg 分开 GROUP BY，在 Controller 按 key 合并，避免 JOIN 行翻倍导致订单级列重复求和。
     */
    @Select("SELECT DATE_FORMAT(o.create_time, #{dateFormat}) AS `key`, " +
            "COALESCE(SUM(oi.qty * COALESCE(p.cost_price, 0)), 0) AS cost " +
            "FROM order_item oi " +
            "JOIN sys_order o ON oi.order_id = o.id " +
            "LEFT JOIN product_sku sku ON oi.sku_id = sku.id " +
            "LEFT JOIN product p ON sku.product_id = p.id " +
            "WHERE o.create_time >= #{start} AND o.create_time < #{end} " +
            "GROUP BY DATE_FORMAT(o.create_time, #{dateFormat})")
    List<Map<String, Object>> selectCostByDate(LocalDateTime start, LocalDateTime end, String dateFormat);

    /**
     * 畅销款：按商品名（order_item 冗余 product_name）合并不同颜色/尺码为一行。
     * LEFT JOIN 保证商品被删后历史销量不丢。
     */
    @Select("SELECT oi.product_name AS productName, " +
            "COALESCE(SUM(oi.qty), 0) AS qty, " +
            "COALESCE(SUM(oi.sub_total), 0) AS sales, " +
            "COALESCE(SUM(oi.qty * COALESCE(p.cost_price, 0)), 0) AS cost " +
            "FROM order_item oi " +
            "JOIN sys_order o ON oi.order_id = o.id " +
            "LEFT JOIN product_sku sku ON oi.sku_id = sku.id " +
            "LEFT JOIN product p ON sku.product_id = p.id " +
            "WHERE o.create_time >= #{start} AND o.create_time < #{end} " +
            "GROUP BY oi.product_name")
    List<Map<String, Object>> selectTopProducts(LocalDateTime start, LocalDateTime end);

    /**
     * 分类销售占比：category_id 为 NULL 的商品归「未分类」，按销售额倒序。
     */
    @Select("SELECT COALESCE(c.name, '未分类') AS categoryName, " +
            "COALESCE(SUM(oi.sub_total), 0) AS sales " +
            "FROM order_item oi " +
            "JOIN sys_order o ON oi.order_id = o.id " +
            "LEFT JOIN product_sku sku ON oi.sku_id = sku.id " +
            "LEFT JOIN product p ON sku.product_id = p.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "WHERE o.create_time >= #{start} AND o.create_time < #{end} " +
            "GROUP BY COALESCE(c.name, '未分类') " +
            "ORDER BY sales DESC")
    List<Map<String, Object>> selectCategorySales(LocalDateTime start, LocalDateTime end);
}
