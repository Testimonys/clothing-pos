package com.huaxing;

import com.huaxing.dto.ProductImportPreview;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.service.ProductImportPreviewService;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/** luohuai codeX generate: verify real XLS/XLSX parsing, variant grouping and zero database mutations. */
class ProductImportPreviewTest extends CatalogTestSupport {
    @Autowired ProductImportPreviewService preview;

    @Test
    void recognizesLegacyHeaderAndSkipsFootersWithoutInventingStockOrSpecs() throws Exception {
        try (Workbook book = new HSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            // luohuai codeX  modify: preview fixtures use the configured confirmed dealer.
            row(sheet, 0, "客商", "新旺角");
            row(sheet, 1, "联系");
            header(sheet, 2);
            row(sheet, 3, 10001, 2000000100012d, "外套", "件", 60, 169, "0无", "0均码", -8, "");
            row(sheet, 4, "", "合计", 1);
            row(sheet, 5, "", "", "第1/1页");
            row(sheet, 6, "\u001a");
            ProductImportPreview result = run(book, "legacy.xls");
            assertThat(result.getTotalRows()).isEqualTo(1);
            assertThat(result.getNeedsSpecRows()).isEqualTo(1);
            assertThat(result.getRows().get(0).getSourceRow()).isEqualTo(4);
            assertThat(result.getRows().get(0).getBarcode()).isEqualTo("2000000100012");
            assertThat(result.getRows().get(0).getSellPrice()).isEqualByComparingTo("169.00");
            assertThat(result.getRows().get(0).getColor()).isEmpty();
            assertThat(products.selectCount(null)).isZero();
            assertThat(skus.selectCount(null)).isZero();
        }
    }

    // luohuai codeX generate: confirmed import creates dealer-bound pending specifications but never inventory.
    @Test
    void confirmedImportWritesCatalogWithoutStock() throws Exception {
        try (Workbook book = new HSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            header(sheet, 0);
            row(sheet, 1, "10001", "2000000100012", "外套", "件", 60, 169, "0无", "0均码");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            book.write(bytes);
            var result = importService.commit(new MockMultipartFile("file", "catalog.xls", "application/vnd.ms-excel", bytes.toByteArray()));
            assertThat(result.productsCreated()).isEqualTo(1);
            assertThat(products.selectList(null).get(0).getDealerId()).isEqualTo(1L);
            assertThat(skus.selectList(null).get(0).getBarcode()).isEqualTo("2000000100012");
            assertThat(skus.selectList(null).get(0).getStockQty()).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_record", Long.class)).isZero();
        }
    }

    @Test
    void xlsxGroupsDistinctVariantsUnderOneArticleAndPreservesLeadingZeros() throws Exception {
        try (Workbook book = new XSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            header(sheet, 0);
            row(sheet, 1, "001001", "0012345678901", "外套", "件", 60, 169, "黑色", "M");
            row(sheet, 2, "001001", "0012345678902", "外套", "件", 60, 169, "白色", "L");
            ProductImportPreview result = run(book, "variants.xlsx");
            assertThat(result.getProductCount()).isEqualTo(1);
            assertThat(result.getReadyRows()).isEqualTo(2);
            assertThat(result.getRows().get(0).getProductCode()).isEqualTo("001001");
            assertThat(result.getRows().get(0).getBarcode()).isEqualTo("0012345678901");
        }
    }

    @Test
    void duplicateBarcodesAndConflictingProductMetadataMarkAllAffectedRows() throws Exception {
        try (Workbook book = new HSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            header(sheet, 0);
            row(sheet, 1, "10001", "duplicate", "外套", "件", 60, 169, "黑色", "M");
            row(sheet, 2, "10001", "duplicate", "外套", "件", 60, 179, "白色", "L");
            ProductImportPreview result = run(book, "invalid.xls");
            assertThat(result.getInvalidRows()).isEqualTo(2);
            assertThat(result.getRows()).allSatisfy(r -> assertThat(r.getErrors()).hasSize(2));
        }
    }

    @Test
    void flagsExistingRecordsWithoutOverwritingTheirPricesOrStock() throws Exception {
        Product existing = catalog.create(product("10001", variant("黑色", "M", "barcode-a", 10)));
        try (Workbook book = new HSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            header(sheet, 0);
            row(sheet, 1, "10001", "barcode-a", "新名称", "件", 10, 20, "白色", "L");
            ProductImportPreview result = run(book, "existing.xls");
            assertThat(result.getConflictRows()).isEqualTo(1);
            assertThat(result.getRows().get(0).getConflicts()).hasSize(2);
            assertThat(products.selectById(existing.getId()).getSellPrice()).isEqualByComparingTo("169");
            assertThat(skus.selectList(null)).extracting(ProductSku::getStockQty).containsExactly(10);
        }
    }

    @Test
    void distinguishesBlankAndZeroPricesAndDoesNotEvaluateFormulas() throws Exception {
        try (Workbook book = new HSSFWorkbook()) {
            Sheet sheet = book.createSheet("商品");
            header(sheet, 0);
            row(sheet, 1, "10001", "a", "零价", "件", 0, 0, "黑色", "均码");
            row(sheet, 2, "10002", "b", "缺价", "件", "", "", "黑色", "M");
            row(sheet, 3, "10003", "c", "公式", "件", 20, 30, "黑色", "M");
            sheet.getRow(3).getCell(5).setCellFormula("1+2");
            ProductImportPreview result = run(book, "prices.xls");
            assertThat(result.getReadyRows()).isEqualTo(1);
            assertThat(result.getInvalidRows()).isEqualTo(2);
            assertThat(result.getRows().get(1).getCostPrice()).isNull();
            assertThat(result.getRows().get(2).getErrors()).anyMatch(s -> s.contains("公式"));
        }
    }

    @Test
    void detectsDuplicateColorSizeEvenAcrossWorksheets() throws Exception {
        try (Workbook book = new HSSFWorkbook()) {
            for (int i = 0; i < 2; i++) {
                Sheet sheet = book.createSheet("商品" + i);
                header(sheet, 0);
                row(sheet, 1, "10001", "barcode-" + i, "外套", "件", 60, 169, "黑色", "M");
            }
            assertThat(run(book, "duplicate-spec.xls").getInvalidRows()).isEqualTo(2);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "store.sample.path", matches = ".+")
    void actualStoreWorkbookHas220ProductsAndNoInferredSpecifications() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(System.getProperty("store.sample.path")));
        ProductImportPreview result = preview.preview(new MockMultipartFile("file", "test111.xls", "application/vnd.ms-excel", bytes));
        assertThat(result.getTotalRows()).isEqualTo(220);
        assertThat(result.getProductCount()).isEqualTo(220);
        assertThat(result.getNeedsSpecRows()).isEqualTo(220);
        assertThat(result.getInvalidRows()).isZero();
        assertThat(result.getConflictRows()).isZero();
        assertThat(result.getRows()).allSatisfy(r -> {
            assertThat(r.getBarcode()).matches("\\d{13}");
            assertThat(r.getColor()).isEmpty();
            assertThat(r.getSize()).isEmpty();
        });
        assertThat(products.selectCount(null)).isZero();
        assertThat(skus.selectCount(null)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stock_record", Long.class)).isZero();
    }

    private ProductImportPreview run(Workbook book, String name) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        book.write(bytes);
        return preview.preview(new MockMultipartFile("file", name, "application/octet-stream", bytes.toByteArray()));
    }

    private void header(Sheet sheet, int index) {
        // luohuai codeX  modify: compact fixtures include required file-level dealer metadata on the header row.
        row(sheet, index, "货号", "条码", "品名", "单位", "成本价", "零售价", "颜色", "尺码", "末入量", "空列", "客商", "新旺角");
    }

    private void row(Sheet sheet, int index, Object... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number number) row.createCell(i).setCellValue(number.doubleValue());
            else row.createCell(i).setCellValue(String.valueOf(values[i]));
        }
    }
}
