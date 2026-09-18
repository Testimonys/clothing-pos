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
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/** luohuai codeX generate: isolated database setup shared by catalog and import regression tests. */
// luohuai codeX  modify: these real-database tests use no mocks; avoid Mockito agent attachment in restricted runtimes.
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@SpringBootTest(classes = CatalogTestSupport.Config.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"spring.datasource.url=jdbc:h2:mem:catalog;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.sql.init.mode=never", "logging.level.root=WARN"})
abstract class CatalogTestSupport {
    @Configuration
    @EnableAutoConfiguration
    @MapperScan("com.huaxing.mapper")
    @Import({MyBatisPlusConfig.class, MyMetaObjectHandler.class, ProductCatalogService.class,
            ProductImportPreviewService.class, ProductController.class})
    static class Config { }

    @Autowired ProductCatalogService catalog;
    @Autowired ProductMapper products;
    @Autowired ProductSkuMapper skus;
    @Autowired ProductController controller;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("DROP ALL OBJECTS");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
    }

    static ProductDTO product(String code, ProductSkuDTO... variants) {
        return ProductDTO.builder().productCode(code).name("样品外套").unit("件").costPrice(new BigDecimal("60.00"))
                .sellPrice(new BigDecimal("169.00")).skus(new ArrayList<>(List.of(variants))).build();
    }

    static ProductSkuDTO variant(String color, String size, String barcode, int stock) {
        return ProductSkuDTO.builder().color(color).size(size).barcode(barcode).stockQty(stock).build();
    }

    static ProductDTO form(Product product) {
        ProductDTO dto = product(product.getProductCode());
        for (ProductSku sku : product.getSkus()) dto.getSkus().add(ProductSkuDTO.builder().id(sku.getId()).color(sku.getColor())
                .size(sku.getSize()).barcode(sku.getBarcode()).stockQty(sku.getStockQty()).build());
        return dto;
    }
}
