package com.huaxing;

import com.huaxing.config.MyBatisPlusConfig;
import com.huaxing.config.MyMetaObjectHandler;
import com.huaxing.controller.ProductController;
import com.huaxing.dto.ProductDTO;
import com.huaxing.dto.ProductSkuDTO;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.mapper.ProductMapper;
import com.huaxing.mapper.ProductSkuMapper;
import com.huaxing.service.ProductCatalogService;
import com.huaxing.service.ProductImportPreviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/** luohuai codeX generate: verify catalog transactions and metadata-only inventory preservation. */
class CatalogIntegrationTest extends CatalogTestSupport {
    @Test
    void createsIndependentColorSizeVariantsAndKeepsArticleNumberAsText() {
        Product product = catalog.create(product("001001", variant("黑色", "M", "0012345678901", 3), variant("黑色", "L", "0012345678902", 8)));
        assertThat(product.getProductCode()).isEqualTo("001001");
        assertThat(product.getUnit()).isEqualTo("件");
        assertThat(product.getSkus()).extracting(ProductSku::getStockQty).containsExactly(3, 8);
        assertThat(products.selectById(product.getId()).getProductCode()).isEqualTo("001001");
    }

    @Test
    void editingMetadataNeverRestoresStockFromAnOldForm() {
        Product original = catalog.create(product("10001", variant("黑色", "M", "2000000100012", 10)));
        ProductDTO form = form(original);
        form.setName("新名称");
        form.getSkus().get(0).setStockQty(10);
        Long skuId = original.getSkus().get(0).getId();
        jdbc.update("UPDATE product_sku SET stock_qty=9, version=version+1 WHERE id=?", skuId);
        catalog.update(original.getId(), form, true);
        assertThat(skus.selectById(skuId).getStockQty()).isEqualTo(9);
        assertThat(products.selectById(original.getId()).getName()).isEqualTo("新名称");
        ProductSkuDTO oldClient = variant("白色", "M", "2000000100012", 999);
        catalog.updateSku(original.getId(), skuId, oldClient);
        assertThat(skus.selectById(skuId).getStockQty()).isEqualTo(9);
        assertThat(skus.selectById(skuId).getColor()).isEqualTo("白色");
    }

    @Test
    void laterDatabaseFailureRollsBackEarlierSkuChanges() {
        Product original = catalog.create(product("10001", variant("黑色", "M", "2000000100012", 0)));
        ProductDTO form = form(original);
        form.getSkus().get(0).setColor("白色");
        form.getSkus().add(variant("白色", "L", "2000000100029", 0));
        form.setCategoryId(99999L);
        assertThatThrownBy(() -> catalog.update(original.getId(), form, true)).isInstanceOf(RuntimeException.class);
        assertThat(skus.selectById(original.getSkus().get(0).getId()).getColor()).isEqualTo("黑色");
        assertThat(skus.selectCount(null)).isEqualTo(1);
        assertThat(products.selectById(original.getId()).getCategoryId()).isNull();
    }

    @Test
    void duplicateVariantsAndBarcodesDoNotCreatePartialProducts() {
        assertThatThrownBy(() -> catalog.create(product("10001", variant("黑色", "M", "a", 0), variant("黑色", "m", "b", 0))))
                .hasMessageContaining("组合重复");
        assertThatThrownBy(() -> catalog.create(product("10001", variant("黑色", "M", "a", 0), variant("白色", "M", "a", 0))))
                .hasMessageContaining("相同条码");
        assertThat(products.selectCount(null)).isZero();
        assertThat(skus.selectCount(null)).isZero();
    }

    @Test
    void rejectsUnknownNewSpecificationsAndMissingBarcodes() {
        assertThatThrownBy(() -> catalog.create(product("10001", variant("0无", "0均码", "abc", 0)))).hasMessageContaining("真实颜色和尺码");
        assertThatThrownBy(() -> catalog.create(product("10001", variant("黑色", "均码", "", 0)))).hasMessageContaining("条码");
        assertThat(products.selectCount(null)).isZero();
    }

    @Test
    void existingLegacyVariantsAndArticleNumbersRemainEditable() {
        jdbc.update("INSERT INTO product (name, product_code, unit, cost_price, sell_price) VALUES ('旧商品','0099','件',20,50)");
        Long productId = jdbc.queryForObject("SELECT id FROM product", Long.class);
        jdbc.update("INSERT INTO product_sku (product_id,color,size,barcode,stock_qty) VALUES (?,?,?,?,?)", productId, "", "", "legacy", 3);
        ProductDTO dto = product(null, variant("", "", "legacy", 0));
        dto.getSkus().get(0).setId(jdbc.queryForObject("SELECT id FROM product_sku", Long.class));
        dto.setUnit(null);
        catalog.update(productId, dto, false);
        assertThat(products.selectById(productId).getProductCode()).isEqualTo("0099");
        dto.setProductCode("0099");
        catalog.update(productId, dto, true);
        assertThat(skus.selectList(null).get(0).getStockQty()).isEqualTo(3);
    }

    @Test
    void cannotRemoveVariantsWithStockOrHistory() {
        Product original = catalog.create(product("10001", variant("黑色", "M", "a", 1), variant("白色", "L", "b", 0)));
        ProductDTO form = form(original);
        form.getSkus().remove(0);
        assertThatThrownBy(() -> catalog.update(original.getId(), form, true)).hasMessageContaining("不能删除");
        Long id = original.getSkus().get(0).getId();
        jdbc.update("UPDATE product_sku SET stock_qty=0 WHERE id=?", id);
        jdbc.update("INSERT INTO stock_record (sku_id,type,qty,before_qty,after_qty) VALUES (?,'OUTBOUND',1,1,0)", id);
        assertThatThrownBy(() -> catalog.update(original.getId(), form, true)).hasMessageContaining("不能删除");
        assertThat(skus.selectCount(null)).isEqualTo(2);
    }

    @Test
    void searchCombinesKeywordAndCategoryWithoutDuplicateProducts() {
        jdbc.update("INSERT INTO category (name) VALUES ('服装柜'), ('鞋柜')");
        Long clothing = jdbc.queryForObject("SELECT id FROM category WHERE name='服装柜'", Long.class);
        Long shoes = jdbc.queryForObject("SELECT id FROM category WHERE name='鞋柜'", Long.class);
        ProductDTO dto = product("001001", variant("黑色", "M", "MATCH1", 0), variant("黑色", "L", "MATCH2", 0));
        dto.setCategoryId(clothing);
        catalog.create(dto);
        ProductDTO other = product("001002", variant("白色", "L", "MATCH3", 0));
        other.setCategoryId(shoes);
        catalog.create(other);
        Map<String, Object> response = controller.list("MATCH", clothing, 0, 20).getBody();
        assertThat(response.get("totalElements")).isEqualTo(1L);
        assertThat((List<?>) response.get("content")).hasSize(1);
        assertThat(controller.list("001001", null, 0, 20).getBody().get("totalElements")).isEqualTo(1L);
        assertThat(controller.list("样品", null, 0, 20).getBody().get("totalElements")).isEqualTo(2L);
    }

    @Test
    void articleNumberConflictsAndInvalidPricesAreRejected() {
        catalog.create(product("10001", variant("黑色", "M", "a", 0)));
        assertThatThrownBy(() -> catalog.create(product("10001", variant("白色", "L", "b", 0)))).hasMessageContaining("货号已存在");
        ProductDTO invalid = product("10002", variant("白色", "L", "b", 0));
        invalid.setSellPrice(new BigDecimal("1.001"));
        assertThatThrownBy(() -> catalog.create(invalid)).hasMessageContaining("两位小数");
    }

    // luohuai codeX generate: direct routes cannot bypass catalog safeguards or reuse pending barcode reservations.
    @Test
    void deletionAndBarcodeEntryPointsRespectCatalogRules() {
        Product product = catalog.create(product("10001", variant("黑色", "M", "old-code", 2)));
        Long skuId = product.getSkus().get(0).getId();
        assertThatThrownBy(() -> catalog.deleteSku(product.getId(), skuId)).hasMessageContaining("不能删除");
        assertThatThrownBy(() -> catalog.deleteProduct(product.getId())).hasMessageContaining("不能删除");
        String reserved = String.valueOf(controller.generateNextBarcode().getBody().get("barcode"));
        Map<?, ?> generated = (Map<?, ?>) controller.generateBarcode(product.getId(), skuId).getBody();
        assertThat(generated.get("barcode")).isNotEqualTo(reserved);
        assertThat(skus.selectById(skuId).getStockQty()).isEqualTo(2);
    }

}
