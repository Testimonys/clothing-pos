package com.huaxing.config;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 MyBatis-Plus IPage 转换为 Spring Data Page 格式，
 * 保持前端兼容（content / totalElements / totalPages / number / size / first / last / empty）
 */
public class PageUtils {

    public static Map<String, Object> convert(IPage<?> page) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getRecords());
        result.put("totalElements", page.getTotal());
        result.put("totalPages", page.getPages());
        result.put("number", page.getCurrent() - 1); // MyBatis-Plus 从1开始, Spring Data 从0开始
        result.put("size", page.getSize());
        result.put("first", page.getCurrent() == 1);
        result.put("last", page.getCurrent() >= page.getPages());
        result.put("empty", page.getRecords() == null || page.getRecords().isEmpty());
        return result;
    }
}
