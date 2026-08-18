package com.huaxing.controller;

import com.huaxing.mapper.ReportMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售统计报表接口（仅老板 BOSS 可访问，见 SecurityConfig）。
 * 数据从现有表实时聚合，只读，不新增表。
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String DAY_FORMAT = "%Y-%m-%d";
    private static final String MONTH_FORMAT = "%Y-%m";

    private final ReportMapper reportMapper;

    public ReportController(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /**
     * GET /api/report/summary
     * 销售汇总 + 趋势：totals（订单数/销售额/实收/让利/毛利/毛利率/客单价）+ trend（按日或按月序列）。
     */
    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam(required = false) String start,
                                     @RequestParam(required = false) String end,
                                     @RequestParam(defaultValue = "day") String granularity) {
        LocalDate[] range;
        try {
            range = parseRange(start, end);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        if (!"day".equals(granularity) && !"month".equals(granularity)) {
            return ResponseEntity.badRequest().body(Map.of("message", "granularity 必须为 day 或 month"));
        }

        String dateFormat = "day".equals(granularity) ? DAY_FORMAT : MONTH_FORMAT;
        LocalDateTime startDT = startDate.atStartOfDay();
        LocalDateTime endDT = endDate.plusDays(1).atStartOfDay();

        // 双查询避免 JOIN 行翻倍：订单级聚合与成本分桶分开 GROUP BY，按 key 合并
        List<Map<String, Object>> orderAgg = reportMapper.selectOrderAgg(startDT, endDT, dateFormat);
        List<Map<String, Object>> costByDate = reportMapper.selectCostByDate(startDT, endDT, dateFormat);

        Map<String, Map<String, Object>> orderMap = new HashMap<>();
        for (Map<String, Object> row : orderAgg) {
            orderMap.put(String.valueOf(row.get("key")), row);
        }
        Map<String, Map<String, Object>> costMap = new HashMap<>();
        for (Map<String, Object> row : costByDate) {
            costMap.put(String.valueOf(row.get("key")), row);
        }

        // 生成完整 key 序列（day 遍历每天 / month 遍历每月），缺失 key 补 0，保证 x 轴连续
        List<String> keys = new ArrayList<>();
        if ("day".equals(granularity)) {
            LocalDate d = startDate;
            while (!d.isAfter(endDate)) {
                keys.add(d.format(DATE_FORMATTER));
                d = d.plusDays(1);
            }
        } else {
            YearMonth ym = YearMonth.from(startDate);
            YearMonth endYm = YearMonth.from(endDate);
            while (!ym.isAfter(endYm)) {
                keys.add(ym.toString());
                ym = ym.plusMonths(1);
            }
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        BigDecimal orderCountSum = BigDecimal.ZERO;
        BigDecimal salesAmountSum = BigDecimal.ZERO;
        BigDecimal payAmountSum = BigDecimal.ZERO;
        BigDecimal discountAmountSum = BigDecimal.ZERO;
        BigDecimal costSum = BigDecimal.ZERO;

        for (String key : keys) {
            Map<String, Object> orderRow = orderMap.get(key);
            Map<String, Object> costRow = costMap.get(key);

            BigDecimal orderCount = orderRow == null ? BigDecimal.ZERO
                    : BigDecimal.valueOf(((Number) orderRow.get("orderCount")).longValue());
            BigDecimal salesAmount = orderRow == null ? BigDecimal.ZERO : toDecimal(orderRow.get("salesAmount"));
            BigDecimal payAmount = orderRow == null ? BigDecimal.ZERO : toDecimal(orderRow.get("payAmount"));
            BigDecimal discountAmount = orderRow == null ? BigDecimal.ZERO : toDecimal(orderRow.get("discountAmount"));
            BigDecimal cost = costRow == null ? BigDecimal.ZERO : toDecimal(costRow.get("cost"));

            BigDecimal grossProfit = payAmount.subtract(cost);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("key", key);
            point.put("sales", scale2(salesAmount));
            point.put("orders", orderCount.longValue());
            point.put("grossProfit", scale2(grossProfit));
            trend.add(point);

            orderCountSum = orderCountSum.add(orderCount);
            salesAmountSum = salesAmountSum.add(salesAmount);
            payAmountSum = payAmountSum.add(payAmount);
            discountAmountSum = discountAmountSum.add(discountAmount);
            costSum = costSum.add(cost);
        }

        BigDecimal grossProfitTotal = payAmountSum.subtract(costSum);
        BigDecimal grossProfitRate = payAmountSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : scale2(grossProfitTotal.multiply(HUNDRED).divide(payAmountSum, 2, RoundingMode.HALF_UP));
        BigDecimal avgOrderValue = orderCountSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : scale2(payAmountSum.divide(orderCountSum, 2, RoundingMode.HALF_UP));

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("orderCount", orderCountSum.longValue());
        totals.put("salesAmount", scale2(salesAmountSum));
        totals.put("payAmount", scale2(payAmountSum));
        totals.put("discountAmount", scale2(discountAmountSum));
        totals.put("grossProfit", scale2(grossProfitTotal));
        totals.put("grossProfitRate", grossProfitRate);
        totals.put("avgOrderValue", avgOrderValue);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totals", totals);
        result.put("trend", trend);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/report/top-products
     * 畅销款 TOP N：按 orderBy（qty/sales/profit）倒序，返回 rank + 合并后的商品行。
     */
    @GetMapping("/top-products")
    public ResponseEntity<?> topProducts(@RequestParam(required = false) String start,
                                         @RequestParam(required = false) String end,
                                         @RequestParam(defaultValue = "10") int limit,
                                         @RequestParam(defaultValue = "qty") String orderBy) {
        LocalDate[] range;
        try {
            range = parseRange(start, end);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        if (!"qty".equals(orderBy) && !"sales".equals(orderBy) && !"profit".equals(orderBy)) {
            return ResponseEntity.badRequest().body(Map.of("message", "orderBy 必须为 qty、sales 或 profit"));
        }
        int limitClipped = Math.max(1, Math.min(50, limit));

        List<Map<String, Object>> rows = reportMapper.selectTopProducts(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        // 每行补毛利 = 销售额 - 成本
        for (Map<String, Object> row : rows) {
            row.put("grossProfit", toDecimal(row.get("sales")).subtract(toDecimal(row.get("cost"))));
        }

        Comparator<Map<String, Object>> comparator;
        if ("sales".equals(orderBy)) {
            comparator = Comparator.comparing((Map<String, Object> a) -> toDecimal(a.get("sales")));
        } else if ("profit".equals(orderBy)) {
            comparator = Comparator.comparing((Map<String, Object> a) -> toDecimal(a.get("grossProfit")));
        } else {
            comparator = Comparator.comparing((Map<String, Object> a) -> toDecimal(a.get("qty")));
        }
        // 次级排序键：并列时按商品名稳定次序，保证 rank 可复现（null 商品名归一为 "null" 不抛 NPE）
        comparator = comparator.thenComparing((Map<String, Object> a) -> String.valueOf(a.get("productName")));
        rows.sort(comparator.reversed());

        int toIndex = Math.min(limitClipped, rows.size());
        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < toIndex; i++) {
            Map<String, Object> row = rows.get(i);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("rank", i + 1);
            out.put("productName", row.get("productName"));
            out.put("qty", ((Number) row.get("qty")).longValue());
            out.put("sales", scale2(toDecimal(row.get("sales"))));
            out.put("grossProfit", scale2(toDecimal(row.get("grossProfit"))));
            top.add(out);
        }
        return ResponseEntity.ok(top);
    }

    /**
     * GET /api/report/category
     * 分类销售占比：percent = 该类销售额 / 总销售额（scale 2）。
     */
    @GetMapping("/category")
    public ResponseEntity<?> category(@RequestParam(required = false) String start,
                                      @RequestParam(required = false) String end) {
        LocalDate[] range;
        try {
            range = parseRange(start, end);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        List<Map<String, Object>> rows = reportMapper.selectCategorySales(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            total = total.add(toDecimal(row.get("sales")));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            for (Map<String, Object> row : rows) {
                BigDecimal sales = toDecimal(row.get("sales"));
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("categoryName", row.get("categoryName"));
                out.put("sales", scale2(sales));
                out.put("percent", scale2(sales.multiply(HUNDRED).divide(total, 2, RoundingMode.HALF_UP)));
                result.add(out);
            }
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 解析并校验日期范围，返回 [startDate, endDate]。
     * 默认值：end = 今天，start = end - 29 天（近 30 天）。
     * 日期格式非法或 start > end 时抛 {@link IllegalArgumentException}（调用方回 400）。
     */
    private LocalDate[] parseRange(String start, String end) {
        try {
            LocalDate endDate = (end == null || end.isEmpty()) ? LocalDate.now() : LocalDate.parse(end, DATE_FORMATTER);
            LocalDate startDate = (start == null || start.isEmpty()) ? endDate.minusDays(29) : LocalDate.parse(start, DATE_FORMATTER);
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("开始日期不能晚于结束日期");
            }
            return new LocalDate[]{startDate, endDate};
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式非法，应为 yyyy-MM-dd");
        }
    }

    /** 统一把聚合 SQL 返回的 Number（Long/BigDecimal/Integer）转 BigDecimal，null 视为 0 */
    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(String.valueOf(value));
    }

    /** 金额统一 scale 2 */
    private BigDecimal scale2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
