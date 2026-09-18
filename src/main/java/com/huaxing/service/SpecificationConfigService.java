package com.huaxing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huaxing.entity.ColorConfig;
import com.huaxing.entity.ProductSku;
import com.huaxing.entity.SizeConfig;
import com.huaxing.mapper.ColorConfigMapper;
import com.huaxing.mapper.ProductSkuMapper;
import com.huaxing.mapper.SizeConfigMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** luohuai codeX generate: color and size dictionaries share immutable 00-99 code allocation and safe renames. */
@Service
public class SpecificationConfigService {
    private final ColorConfigMapper colors;
    private final SizeConfigMapper sizes;
    private final ProductSkuMapper skus;

    public SpecificationConfigService(ColorConfigMapper colors, SizeConfigMapper sizes, ProductSkuMapper skus) {
        this.colors = colors;
        this.sizes = sizes;
        this.skus = skus;
    }

    public List<ColorConfig> listColors(boolean enabledOnly) {
        LambdaQueryWrapper<ColorConfig> query = new LambdaQueryWrapper<ColorConfig>().orderByAsc(ColorConfig::getSortOrder).orderByAsc(ColorConfig::getCode);
        if (enabledOnly) query.eq(ColorConfig::getEnabled, true);
        return colors.selectList(query);
    }

    public List<SizeConfig> listSizes(boolean enabledOnly) {
        LambdaQueryWrapper<SizeConfig> query = new LambdaQueryWrapper<SizeConfig>().orderByAsc(SizeConfig::getSortOrder).orderByAsc(SizeConfig::getCode);
        if (enabledOnly) query.eq(SizeConfig::getEnabled, true);
        return sizes.selectList(query);
    }

    @Transactional
    public synchronized ColorConfig createColor(ColorConfig request) {
        String name = validName(request == null ? null : request.getName(), "颜色");
        if (colors.selectCount(new LambdaQueryWrapper<ColorConfig>().eq(ColorConfig::getName, name)) > 0) throw conflict("颜色名称已存在");
        ColorConfig value = ColorConfig.builder().code(nextCode(colors.findMaxCode(), "颜色"))
                .name(name).enabled(true).sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder()).build();
        colors.insert(value);
        return value;
    }

    @Transactional
    public synchronized SizeConfig createSize(SizeConfig request) {
        String name = validName(request == null ? null : request.getName(), "尺码");
        if (sizes.selectCount(new LambdaQueryWrapper<SizeConfig>().eq(SizeConfig::getName, name)) > 0) throw conflict("尺码名称已存在");
        SizeConfig value = SizeConfig.builder().code(nextCode(sizes.findMaxCode(), "尺码"))
                .name(name).enabled(true).sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder()).build();
        sizes.insert(value);
        return value;
    }

    @Transactional
    public ColorConfig updateColor(Long id, ColorConfig request) {
        ColorConfig value = colors.selectById(id);
        if (value == null) throw notFound("颜色不存在");
        String oldName = value.getName();
        String name = validName(request.getName(), "颜色");
        if (colors.selectCount(new LambdaQueryWrapper<ColorConfig>().eq(ColorConfig::getName, name).ne(ColorConfig::getId, id)) > 0) throw conflict("颜色名称已存在");
        value.setName(name);
        value.setSortOrder(request.getSortOrder() == null ? value.getSortOrder() : request.getSortOrder());
        if (request.getEnabled() != null) value.setEnabled(request.getEnabled());
        colors.updateById(value);
        // luohuai codeX  modify: keep denormalized SKU labels aligned while the immutable config ID/code stays stable.
        if (!oldName.equals(name)) skus.update(null, new LambdaUpdateWrapper<ProductSku>().eq(ProductSku::getColorConfigId, id).set(ProductSku::getColor, name));
        return value;
    }

    @Transactional
    public SizeConfig updateSize(Long id, SizeConfig request) {
        SizeConfig value = sizes.selectById(id);
        if (value == null) throw notFound("尺码不存在");
        String oldName = value.getName();
        String name = validName(request.getName(), "尺码");
        if (sizes.selectCount(new LambdaQueryWrapper<SizeConfig>().eq(SizeConfig::getName, name).ne(SizeConfig::getId, id)) > 0) throw conflict("尺码名称已存在");
        value.setName(name);
        value.setSortOrder(request.getSortOrder() == null ? value.getSortOrder() : request.getSortOrder());
        if (request.getEnabled() != null) value.setEnabled(request.getEnabled());
        sizes.updateById(value);
        // luohuai codeX  modify: keep denormalized SKU labels aligned while the immutable config ID/code stays stable.
        if (!oldName.equals(name)) skus.update(null, new LambdaUpdateWrapper<ProductSku>().eq(ProductSku::getSizeConfigId, id).set(ProductSku::getSize, name));
        return value;
    }

    @Transactional
    public void deleteColor(Long id) {
        ColorConfig value = colors.selectById(id);
        if (value == null) throw notFound("颜色不存在");
        // luohuai codeX  modify: never recycle an issued color code; disable the record instead.
        throw conflict("颜色编码永久保留，请改为停用");
    }

    @Transactional
    public void deleteSize(Long id) {
        SizeConfig value = sizes.selectById(id);
        if (value == null) throw notFound("尺码不存在");
        // luohuai codeX  modify: never recycle an issued size code; disable the record instead.
        throw conflict("尺码编码永久保留，请改为停用");
    }

    public ColorConfig requireColor(Long id, boolean requireEnabled) {
        ColorConfig value = id == null ? null : colors.selectById(id);
        if (value == null || (requireEnabled && !Boolean.TRUE.equals(value.getEnabled()))) throw invalid("请选择启用的颜色");
        return value;
    }

    public SizeConfig requireSize(Long id, boolean requireEnabled) {
        SizeConfig value = id == null ? null : sizes.selectById(id);
        if (value == null || (requireEnabled && !Boolean.TRUE.equals(value.getEnabled()))) throw invalid("请选择启用的尺码");
        return value;
    }

    public ColorConfig findColorByName(String name) {
        return colors.selectOne(new LambdaQueryWrapper<ColorConfig>().eq(ColorConfig::getName, ProductCatalogService.text(name)).last("LIMIT 1"));
    }

    public SizeConfig findSizeByName(String name) {
        return sizes.selectOne(new LambdaQueryWrapper<SizeConfig>().eq(SizeConfig::getName, ProductCatalogService.text(name)).last("LIMIT 1"));
    }

    private static String nextCode(String max, String label) {
        int next = max == null ? 1 : Integer.parseInt(max) + 1;
        if (next > 99) throw invalid(label + "编码已用完（01-99）");
        return String.format("%02d", next);
    }
    private static String validName(String name, String label) {
        String value = ProductCatalogService.text(name);
        if (value.isEmpty() || value.length() > 50) throw invalid(label + "名称必填且不超过50字");
        return value;
    }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
