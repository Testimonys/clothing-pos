package com.huaxing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaxing.dto.ProductDTO;
import com.huaxing.dto.ProductSkuDTO;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.mapper.ProductMapper;
import com.huaxing.mapper.ProductSkuMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** luohuai codeX generate: validate color/size catalogs and save an entire product edit in one transaction. */
@Service
public class ProductCatalogService {
    private final ProductMapper products;
    private final ProductSkuMapper skus;

    public ProductCatalogService(ProductMapper products, ProductSkuMapper skus) {
        this.products = products;
        this.skus = skus;
    }

    @Transactional
    public Product create(ProductDTO dto) {
        validateProduct(dto, null);
        List<ProductSkuDTO> desired = requireSkus(dto);
        validateSkus(desired, Collections.emptyMap());
        Product product = new Product();
        copyProduct(dto, product);
        products.insert(product);
        for (ProductSkuDTO item : desired) {
            skus.insert(newSku(product.getId(), item));
        }
        return withSkus(product);
    }

    @Transactional
    public Product update(Long id, ProductDTO dto, boolean includeSkus) {
        Product product = requireProduct(id);
        // luohuai codeX  modify: older metadata clients omit the newly added fields; preserve their stored values.
        if (!includeSkus) {
            if (dto.getProductCode() == null) dto.setProductCode(product.getProductCode());
            if (dto.getUnit() == null) dto.setUnit(product.getUnit());
        }
        validateProduct(dto, id);
        List<ProductSku> existing = listSkus(id);
        if (includeSkus) {
            List<ProductSkuDTO> desired = requireSkus(dto);
            Map<Long, ProductSku> byId = existing.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
            validateSkus(desired, byId);
            Set<Long> kept = desired.stream().map(ProductSkuDTO::getId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (ProductSku old : existing) {
                if (!kept.contains(old.getId())) requireDeletable(old);
            }
            for (ProductSku old : existing) {
                if (!kept.contains(old.getId())) skus.deleteById(old.getId());
            }
            for (ProductSkuDTO item : desired) {
                if (item.getId() == null) skus.insert(newSku(id, item));
                else updateSkuMetadata(id, item);
            }
        }
        copyProduct(dto, product);
        products.updateById(product);
        return withSkus(product);
    }

    @Transactional
    public ProductSku addSku(Long productId, ProductSkuDTO dto) {
        requireProduct(productId);
        dto.setId(null);
        // luohuai codeX  modify: reuse one consistent list while the product lock is held.
        List<ProductSku> current = listSkus(productId);
        List<ProductSkuDTO> desired = current.stream().map(this::toDto).collect(Collectors.toList());
        Map<Long, ProductSku> existing = current.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
        desired.add(dto);
        validateSkus(desired, existing);
        ProductSku sku = newSku(productId, dto);
        skus.insert(sku);
        return sku;
    }

    @Transactional
    public ProductSku updateSku(Long productId, Long skuId, ProductSkuDTO dto) {
        requireProduct(productId);
        List<ProductSku> existing = listSkus(productId);
        Map<Long, ProductSku> byId = existing.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
        if (!byId.containsKey(skuId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "规格不存在");
        dto.setId(skuId);
        List<ProductSkuDTO> desired = existing.stream().map(s -> s.getId().equals(skuId) ? dto : toDto(s)).collect(Collectors.toList());
        validateSkus(desired, byId);
        updateSkuMetadata(productId, dto);
        return skus.selectById(skuId);
    }

    /** luohuai codeX generate: apply the same history and stock protections to standalone deletion routes. */
    @Transactional
    public void deleteSku(Long productId, Long skuId) {
        requireProduct(productId);
        ProductSku sku = skus.selectById(skuId);
        if (sku == null || !productId.equals(sku.getProductId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "规格不存在");
        requireDeletable(sku);
        skus.deleteById(skuId);
    }

    @Transactional
    public void deleteProduct(Long productId) {
        requireProduct(productId);
        List<ProductSku> current = listSkus(productId);
        current.forEach(this::requireDeletable);
        skus.delete(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId));
        products.deleteById(productId);
    }

    private void requireDeletable(ProductSku sku) {
        if ((sku.getStockQty() != null && sku.getStockQty() != 0) || skus.countHistory(sku.getId()) > 0) {
            throw conflict("有库存或交易流水的规格不能删除：" + spec(sku.getColor(), sku.getSize()));
        }
    }

    private void updateSkuMetadata(Long productId, ProductSkuDTO item) {
        ProductSku patch = new ProductSku();
        patch.setId(item.getId());
        patch.setProductId(productId);
        patch.setColor(item.getColor());
        patch.setSize(item.getSize());
        patch.setBarcode(blankToNull(item.getBarcode()));
        if (skus.updateMetadata(patch) != 1) throw conflict("规格已变化，请刷新后重试");
    }

    private void validateProduct(ProductDTO dto, Long id) {
        if (dto == null) throw invalid("商品资料不能为空");
        dto.setName(text(dto.getName()));
        if (dto.getName().isEmpty() || dto.getName().length() > 200) throw invalid("商品名称必填且不超过200字");
        dto.setProductCode(blankToNull(dto.getProductCode()));
        if (dto.getProductCode() != null && dto.getProductCode().length() > 50) throw invalid("货号不能超过50字");
        dto.setUnit(text(dto.getUnit()).isEmpty() ? "件" : text(dto.getUnit()));
        if (dto.getUnit().length() > 20) throw invalid("单位不能超过20字");
        validatePrice(dto.getCostPrice(), "成本价", false);
        validatePrice(dto.getSellPrice(), "零售价", true);
        if (dto.getProductCode() != null) {
            LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getProductCode, dto.getProductCode());
            if (id != null) query.ne(Product::getId, id);
            if (products.selectCount(query) > 0) throw conflict("货号已存在：" + dto.getProductCode());
        }
    }

    private List<ProductSkuDTO> requireSkus(ProductDTO dto) {
        if (dto.getSkus() == null || dto.getSkus().isEmpty()) throw invalid("请至少添加一个颜色和尺码规格");
        if (dto.getSkus().size() > 500) throw invalid("单个商品最多支持500个规格");
        return dto.getSkus();
    }

    private void validateSkus(List<ProductSkuDTO> desired, Map<Long, ProductSku> existing) {
        Set<String> combinations = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (ProductSkuDTO item : desired) {
            if (item == null) throw invalid("规格不能为空");
            ProductSku old = item.getId() == null ? null : existing.get(item.getId());
            if (item.getId() != null && (old == null || !ids.add(item.getId()))) throw invalid("规格ID不属于该商品或重复");
            item.setColor(text(item.getColor()));
            item.setSize(text(item.getSize()));
            item.setBarcode(blankToNull(item.getBarcode()));
            boolean unchangedLegacy = old != null && text(old.getColor()).equals(item.getColor()) && text(old.getSize()).equals(item.getSize());
            if (!unchangedLegacy && (missingColor(item.getColor()) || missingSize(item.getSize()))) {
                throw invalid("新增或修改规格必须填写真实颜色和尺码；均码商品请填写“均码”");
            }
            if (item.getColor().length() > 50 || item.getSize().length() > 50) throw invalid("颜色和尺码不能超过50字");
            String key = item.getColor().toLowerCase(Locale.ROOT) + "\u0000" + item.getSize().toLowerCase(Locale.ROOT);
            if (!combinations.add(key)) throw invalid("颜色和尺码组合重复：" + spec(item.getColor(), item.getSize()));
            if (item.getId() == null && item.getStockQty() != null && item.getStockQty() < 0) throw invalid("初始库存不能为负数");
            // luohuai codeX  modify: new variants need independent scannable identities; preserve incomplete legacy SKUs.
            if (item.getId() == null && item.getBarcode() == null) throw invalid("新规格请填写条码，或使用补全空条码");
            if (item.getBarcode() != null) {
                if (item.getBarcode().length() > 100 || !barcodes.add(item.getBarcode().toLowerCase(Locale.ROOT))) throw invalid("条码过长或同一商品的多个规格使用相同条码");
                ProductSku occupied = skus.findByBarcode(item.getBarcode()).orElse(null);
                if (occupied != null && !occupied.getId().equals(item.getId())) throw conflict("条码已被其他规格使用：" + item.getBarcode());
            }
        }
    }

    public static void validatePrice(BigDecimal price, String name, boolean required) {
        if (price == null) {
            if (required) throw invalid(name + "不能为空");
            return;
        }
        if (price.signum() < 0 || price.compareTo(new BigDecimal("99999999.99")) > 0 || price.stripTrailingZeros().scale() > 2) {
            throw invalid(name + "必须为非负金额，最多两位小数且不超过99999999.99");
        }
    }

    private Product requireProduct(Long id) {
        Product product = products.lockById(id);
        if (product == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "商品不存在");
        return product;
    }

    private void copyProduct(ProductDTO dto, Product product) {
        product.setName(dto.getName());
        product.setProductCode(dto.getProductCode());
        product.setUnit(dto.getUnit());
        product.setCategoryId(dto.getCategoryId());
        product.setImageUrl(dto.getImageUrl());
        product.setCostPrice(dto.getCostPrice());
        product.setSellPrice(dto.getSellPrice());
    }

    private ProductSku newSku(Long productId, ProductSkuDTO dto) {
        return ProductSku.builder().productId(productId).color(dto.getColor()).size(dto.getSize())
                .barcode(dto.getBarcode()).stockQty(dto.getStockQty() == null ? 0 : dto.getStockQty()).version(0L).build();
    }

    private List<ProductSku> listSkus(Long productId) {
        return skus.selectList(new LambdaQueryWrapper<ProductSku>().eq(ProductSku::getProductId, productId).orderByAsc(ProductSku::getId));
    }

    private Product withSkus(Product product) {
        product.setSkus(listSkus(product.getId()));
        return product;
    }

    private ProductSkuDTO toDto(ProductSku sku) {
        return ProductSkuDTO.builder().id(sku.getId()).color(sku.getColor()).size(sku.getSize()).barcode(sku.getBarcode()).build();
    }

    public static String text(String value) { return value == null ? "" : value.trim(); }
    public static String blankToNull(String value) { return text(value).isEmpty() ? null : text(value); }
    public static boolean missingColor(String value) { return text(value).isEmpty() || "0无".equals(text(value)); }
    public static boolean missingSize(String value) { return text(value).isEmpty() || "0均码".equals(text(value)); }
    private static String spec(String color, String size) { return text(color) + " / " + text(size); }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
