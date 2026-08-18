package com.huaxing.config;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 MyBatis-Plus IPage 转换为 Spring Data Page 格式，
 * 保持前端兼容（content / totalElements / totalPages / number / size / first / last / empty）
 */
public class PageUtils {

    public static Map<String, Object> convert(IPage<?> page) {
        return convertWithRecords(page, page.getRecords());
    }

    /**
     * 使用自定义的 content 列表构建 Spring Data Page 格式。
     * 适用于需要把实体转换为 DTO/Map 后再返回分页的场景。
     */
    public static Map<String, Object> convertWithRecords(IPage<?> page, List<?> content) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalElements", page.getTotal());
        result.put("totalPages", page.getPages());
        result.put("number", page.getCurrent() - 1); // MyBatis-Plus 从1开始, Spring Data 从0开始
        result.put("size", page.getSize());
        result.put("first", page.getCurrent() == 1);
        result.put("last", page.getCurrent() >= page.getPages());
        result.put("empty", content == null || content.isEmpty());
        return result;
    }
}
