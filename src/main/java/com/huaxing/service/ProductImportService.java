package com.huaxing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaxing.dto.ProductImportPreview;
import com.huaxing.dto.ProductImportResult;
import com.huaxing.entity.*;
import com.huaxing.mapper.CategoryMapper;
import com.huaxing.mapper.ProductMapper;
import com.huaxing.mapper.ProductSkuMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/** luohuai codeX generate: commit a freshly revalidated preview without importing stock or overwriting catalog rows. */
@Service
public class ProductImportService {
    private final ProductImportPreviewService previews;
    private final ProductMapper products;
    private final ProductSkuMapper skus;
    private final CategoryMapper categories;
    private final SpecificationConfigService specifications;

    public ProductImportService(ProductImportPreviewService previews, ProductMapper products, ProductSkuMapper skus,
                                CategoryMapper categories, SpecificationConfigService specifications) {
        this.previews = previews;
        this.products = products;
        this.skus = skus;
        this.categories = categories;
        this.specifications = specifications;
    }

    @Transactional
    public ProductImportResult commit(MultipartFile file) {
        ProductImportPreview preview = previews.preview(file);
        if (preview.getDealerId() == null) throw invalid("导入文件中的经销商尚未建立");
        if (preview.getInvalidRows() > 0 || preview.getConflictRows() > 0) throw conflict("预览仍有数据错误或已有冲突，未执行导入");

        Map<String, List<ProductImportPreview.Row>> groups = preview.getRows().stream()
                .collect(Collectors.groupingBy(ProductImportPreview.Row::getProductCode, LinkedHashMap::new, Collectors.toList()));
        int skuCount = 0;
        for (List<ProductImportPreview.Row> rows : groups.values()) {
            ProductImportPreview.Row first = rows.get(0);
            Category category = ProductCatalogService.text(first.getCategoryName()).isEmpty() ? null
                    : categories.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getName, first.getCategoryName()).last("LIMIT 1"));
            Product product = Product.builder().dealerId(preview.getDealerId()).productCode(first.getProductCode())
                    .name(first.getName()).unit(first.getUnit()).categoryId(category == null ? null : category.getId())
                    .costPrice(first.getCostPrice()).sellPrice(first.getSellPrice()).build();
            products.insert(product);
            for (ProductImportPreview.Row row : rows) {
                ColorConfig color = row.getColor().isEmpty() ? null : specifications.findColorByName(row.getColor());
                SizeConfig size = row.getSize().isEmpty() ? null : specifications.findSizeByName(row.getSize());
                ProductSku sku = ProductSku.builder().productId(product.getId()).color(row.getColor()).size(row.getSize())
                        .colorConfigId(color == null ? null : color.getId()).sizeConfigId(size == null ? null : size.getId())
                        .barcode(row.getBarcode()).stockQty(0).version(0L).build();
                skus.insert(sku);
                skuCount++;
            }
        }
        return new ProductImportResult(groups.size(), skuCount, preview.getDealerName());
    }

    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
