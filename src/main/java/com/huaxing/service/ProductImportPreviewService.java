package com.huaxing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huaxing.dto.ProductImportPreview;
import com.huaxing.entity.Product;
import com.huaxing.entity.ProductSku;
import com.huaxing.entity.Dealer;
import com.huaxing.entity.ColorConfig;
import com.huaxing.entity.SizeConfig;
import com.huaxing.mapper.ProductMapper;
import com.huaxing.mapper.ProductSkuMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** luohuai codeX generate: parse only known nonempty catalog columns from XLS/XLSX, with no import or stock writes. */
@Service
public class ProductImportPreviewService {
    private static final int MAX_ROWS = 5000;
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> HEADERS = Map.ofEntries(
            Map.entry("货号", "code"), Map.entry("商品货号", "code"),
            Map.entry("条码", "barcode"), Map.entry("品名", "name"), Map.entry("商品名称", "name"),
            Map.entry("单位", "unit"), Map.entry("成本价", "cost"), Map.entry("进价", "cost"),
            Map.entry("零售价", "price"), Map.entry("售价", "price"), Map.entry("属柜", "category"),
            Map.entry("分类", "category"), Map.entry("颜色", "color"), Map.entry("尺码", "size"), Map.entry("尺寸", "size"));
    private final ProductMapper products;
    private final ProductSkuMapper skus;
    private final DealerService dealers;
    private final SpecificationConfigService specifications;

    public ProductImportPreviewService(ProductMapper products, ProductSkuMapper skus, DealerService dealers,
                                       SpecificationConfigService specifications) {
        this.products = products;
        this.skus = skus;
        this.dealers = dealers;
        this.specifications = specifications;
    }

    public ProductImportPreview preview(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("请选择商品表格");
        if (file.getSize() > MAX_BYTES) throw invalid("商品表格不能超过5MB");
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xls") && !filename.endsWith(".xlsx")) throw invalid("仅支持 XLS 或 XLSX 文件");
        ProductImportPreview result = new ProductImportPreview();
        try (Workbook book = WorkbookFactory.create(file.getInputStream())) {
            if (book.getNumberOfSheets() > 20) throw invalid("最多支持20个工作表");
            for (Sheet sheet : book) readSheet(sheet, result);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw invalid("表格无法读取，请使用未加密、未损坏的 XLS/XLSX 文件");
        }
        if (result.getRows().isEmpty()) throw invalid("未找到商品记录，表头需包含“货号”和“品名/商品名称”");
        // luohuai codeX  modify: imports use the confirmed file-level 客商 as dealer provenance.
        Dealer dealer = dealers.findByName(result.getDealerName());
        if (dealer == null || !Boolean.TRUE.equals(dealer.getEnabled())) {
            result.getRows().forEach(row -> row.getErrors().add("经销商不存在或已停用，请先在经销商管理中创建：" + ProductCatalogService.text(result.getDealerName())));
        } else {
            result.setDealerId(dealer.getId());
            result.setDealerCode(dealer.getCode());
        }
        checkGroups(result.getRows());
        checkExisting(result);
        summarize(result);
        result.getNotes().add("本次仅预览，不写入商品、订单或库存；末入量不作为当前库存。");
        result.getNotes().add("同一货号按颜色和尺码拆分，每个规格需独立条码。原表的0无、0均码标为待补充，不自动生成规格。");
        return result;
    }

    private void readSheet(Sheet sheet, ProductImportPreview result) {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Map<String, Integer> columns = null;
        int header = -1;
        for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 49); i++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
            if (row == null) continue;
            Map<String, Integer> found = new HashMap<>();
            for (int c = 0; c < Math.min(row.getLastCellNum(), 200); c++) {
                String key = HEADERS.get(formatter.formatCellValue(row.getCell(c)).trim());
                if (key != null) {
                    if (found.putIfAbsent(key, c) != null) throw invalid("工作表“" + sheet.getSheetName() + "”存在重复字段表头");
                }
            }
            if (found.containsKey("code") && found.containsKey("name")) {
                columns = found;
                header = i;
                break;
            }
        }
        if (columns == null) {
            result.getNotes().add("工作表“" + sheet.getSheetName() + "”未找到商品表头，已跳过。");
            return;
        }
        // luohuai codeX generate: capture the last nonempty value on the 客商 header row (code may appear before name).
        String dealerName = findDealerName(sheet, header, formatter);
        if (!dealerName.isEmpty()) {
            if (result.getDealerName() != null && !result.getDealerName().equals(dealerName)) throw invalid("同一文件包含多个不同经销商");
            result.setDealerName(dealerName);
        }
        if (sheet.getLastRowNum() - header > MAX_ROWS + 50) throw invalid("单表最多支持5000条商品，请拆分文件");
        for (int i = header + 1; i <= sheet.getLastRowNum(); i++) {
            org.apache.poi.ss.usermodel.Row source = sheet.getRow(i);
            if (source == null) continue;
            ProductImportPreview.Row row = new ProductImportPreview.Row();
            row.setSheet(sheet.getSheetName());
            row.setSourceRow(i + 1);
            String code = read(source, columns, "code", formatter, row, true);
            String barcode = read(source, columns, "barcode", formatter, row, true);
            String name = read(source, columns, "name", formatter, row, false);
            if (row.getErrors().isEmpty() && isFooterOrBlank(source, formatter, code, barcode, name)) continue;
            if ("货号".equals(code) && ("品名".equals(name) || "商品名称".equals(name))) continue;
            row.setProductCode(code);
            row.setBarcode(barcode);
            row.setName(name);
            String unit = read(source, columns, "unit", formatter, row, false);
            row.setUnit(unit.isEmpty() ? "件" : unit);
            row.setCategoryName(read(source, columns, "category", formatter, row, false));
            String color = read(source, columns, "color", formatter, row, false);
            String size = read(source, columns, "size", formatter, row, false);
            row.setColor(ProductCatalogService.missingColor(color) ? "" : color);
            row.setSize(ProductCatalogService.missingSize(size) ? "" : size);
            row.setCostPrice(price(source, columns, "cost", "成本价", formatter, row));
            row.setSellPrice(price(source, columns, "price", "零售价", formatter, row));
            required(code, "货号", 6, row);
            if (!code.isEmpty() && !code.matches("\\d{1,6}")) row.getErrors().add("货号必须是1-6位数字");
            required(barcode, "条码", 100, row);
            required(name, "品名", 200, row);
            if (row.getUnit().length() > 20 || row.getColor().length() > 50 || row.getSize().length() > 50) row.getErrors().add("单位、颜色或尺码超出允许长度");
            if (row.getColor().isEmpty() || row.getSize().isEmpty()) row.getWarnings().add("缺少真实颜色或尺码，请在源文件补充；不同规格不能共用原条码");
            if (!row.getColor().isEmpty()) {
                ColorConfig colorConfig = specifications.findColorByName(row.getColor());
                if (colorConfig == null || !Boolean.TRUE.equals(colorConfig.getEnabled())) row.getErrors().add("颜色未在系统设置中启用：" + row.getColor());
            }
            if (!row.getSize().isEmpty()) {
                SizeConfig sizeConfig = specifications.findSizeByName(row.getSize());
                if (sizeConfig == null || !Boolean.TRUE.equals(sizeConfig.getEnabled())) row.getErrors().add("尺码未在系统设置中启用：" + row.getSize());
            }
            result.getRows().add(row);
            if (result.getRows().size() > MAX_ROWS) throw invalid("单次最多预览5000条商品规格");
        }
    }

    private String findDealerName(Sheet sheet, int header, DataFormatter formatter) {
        // luohuai codeX  modify: include the header row for compact exports that place 客商 metadata after business columns.
        for (int r = 0; r <= header; r++) {
            org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                if (!"客商".equals(formatter.formatCellValue(row.getCell(c)).trim())) continue;
                List<String> values = new ArrayList<>();
                for (int next = c + 1; next < row.getLastCellNum(); next++) {
                    String candidate = formatter.formatCellValue(row.getCell(next)).trim();
                    if (!candidate.isEmpty()) values.add(candidate);
                    if (values.size() == 2) break;
                }
                // luohuai codeX  modify: legacy exports place numeric 客商 code before name; compact files may contain name only.
                if (values.size() >= 2 && values.get(0).matches("\\d+")) return values.get(1);
                return values.isEmpty() ? "" : values.get(0);
            }
        }
        return "";
    }

    private boolean isFooterOrBlank(org.apache.poi.ss.usermodel.Row source, DataFormatter formatter, String code, String barcode, String name) {
        if (code.isEmpty() && ("合计".equals(barcode) || "总计".equals(barcode))) return true;
        if (code.isEmpty() && barcode.isEmpty() && (name.matches("第\\s*\\d+\\s*/\\s*\\d+\\s*页") || "合计".equals(name))) return true;
        for (Cell cell : source) {
            if (!formatter.formatCellValue(cell).replace("\u001a", "").trim().isEmpty()) return false;
        }
        return true;
    }

    private String read(org.apache.poi.ss.usermodel.Row source, Map<String, Integer> columns, String key,
                        DataFormatter formatter, ProductImportPreview.Row row, boolean identifier) {
        Integer col = columns.get(key);
        Cell cell = col == null ? null : source.getCell(col);
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        if (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR) {
            row.getErrors().add("列“" + key + "”包含公式或错误值，请粘贴为值后重试");
            return "";
        }
        String value = formatter.formatCellValue(cell).replace("\u001a", "").trim();
        if (identifier && cell.getCellType() == CellType.NUMERIC) {
            BigDecimal number = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            if (DateUtil.isCellDateFormatted(cell) || number.scale() > 0 || number.signum() < 0 || number.toPlainString().length() > 15) {
                row.getErrors().add("货号/条码必须为文本或不超过15位的整数；长编号请设置为文本");
            } else if (!value.matches("\\d+")) value = number.toPlainString();
        }
        return value;
    }

    private BigDecimal price(org.apache.poi.ss.usermodel.Row source, Map<String, Integer> columns, String key,
                             String label, DataFormatter formatter, ProductImportPreview.Row row) {
        String value = read(source, columns, key, formatter, row, false);
        Cell cell = columns.containsKey(key) ? source.getCell(columns.get(key)) : null;
        try {
            if (value.isEmpty()) throw new IllegalArgumentException();
            BigDecimal amount = cell != null && cell.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(cell.getNumericCellValue()) : new BigDecimal(value);
            ProductCatalogService.validatePrice(amount, label, true);
            return amount.setScale(2);
        } catch (IllegalArgumentException | ArithmeticException | ResponseStatusException error) {
            row.getErrors().add(label + "缺失或不是有效的非负两位小数金额");
            return null;
        }
    }

    private void required(String value, String label, int max, ProductImportPreview.Row row) {
        if (value.isEmpty()) row.getErrors().add(label + "不能为空");
        else if (value.length() > max) row.getErrors().add(label + "长度超过" + max);
    }

    private void checkGroups(List<ProductImportPreview.Row> rows) {
        Map<String, List<ProductImportPreview.Row>> byCode = rows.stream().filter(r -> !r.getProductCode().isEmpty())
                .collect(Collectors.groupingBy(r -> normalize(r.getProductCode())));
        for (List<ProductImportPreview.Row> group : byCode.values()) {
            ProductImportPreview.Row first = group.get(0);
            boolean different = group.stream().anyMatch(r -> !Objects.equals(r.getName(), first.getName()) || !Objects.equals(r.getUnit(), first.getUnit())
                    || !Objects.equals(r.getCostPrice(), first.getCostPrice()) || !Objects.equals(r.getSellPrice(), first.getSellPrice())
                    || !Objects.equals(r.getCategoryName(), first.getCategoryName()));
            if (different) group.forEach(r -> r.getErrors().add("同一货号的名称、单位、分类或价格不一致，请统一商品资料"));
            markDuplicates(group.stream().filter(r -> !r.getColor().isEmpty() && !r.getSize().isEmpty()).collect(Collectors.toList()),
                    r -> normalize(r.getColor()) + "\u0000" + normalize(r.getSize()), "同一货号存在重复颜色和尺码组合");
        }
        markDuplicates(rows.stream().filter(r -> !r.getBarcode().isEmpty()).collect(Collectors.toList()),
                r -> normalize(r.getBarcode()), "文件中条码重复，每个颜色/尺码规格须有独立条码");
    }

    private void markDuplicates(List<ProductImportPreview.Row> rows, Function<ProductImportPreview.Row, String> key, String message) {
        rows.stream().collect(Collectors.groupingBy(key)).values().stream().filter(group -> group.size() > 1)
                .forEach(group -> group.forEach(row -> row.getErrors().add(message)));
    }

    private void checkExisting(ProductImportPreview result) {
        List<ProductImportPreview.Row> rows = result.getRows();
        List<String> codes = rows.stream().map(ProductImportPreview.Row::getProductCode).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
        List<String> barcodes = rows.stream().map(ProductImportPreview.Row::getBarcode).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList());
        Set<String> existingCodes = new HashSet<>();
        Set<String> existingBarcodes = new HashSet<>();
        for (int i = 0; i < codes.size(); i += 500) {
            LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                    .in(Product::getProductCode, codes.subList(i, Math.min(i + 500, codes.size())));
            if (result.getDealerId() != null) query.eq(Product::getDealerId, result.getDealerId());
            products.selectList(query)
                    .forEach(p -> existingCodes.add(normalize(p.getProductCode())));
        }
        for (int i = 0; i < barcodes.size(); i += 500) {
            skus.selectList(new LambdaQueryWrapper<ProductSku>().in(ProductSku::getBarcode, barcodes.subList(i, Math.min(i + 500, barcodes.size()))))
                    .forEach(s -> existingBarcodes.add(normalize(s.getBarcode())));
        }
        for (ProductImportPreview.Row row : rows) {
            if (existingCodes.contains(normalize(row.getProductCode()))) row.getConflicts().add("货号已存在，需人工核对已有商品，预览不会覆盖");
            if (existingBarcodes.contains(normalize(row.getBarcode()))) row.getConflicts().add("条码已存在，预览不会覆盖该规格");
        }
    }

    private void summarize(ProductImportPreview result) {
        result.setTotalRows(result.getRows().size());
        result.setProductCount((int) result.getRows().stream().map(ProductImportPreview.Row::getProductCode).filter(s -> !s.isEmpty()).map(this::normalize).distinct().count());
        for (ProductImportPreview.Row row : result.getRows()) {
            if (!row.getErrors().isEmpty()) { row.setStatus("INVALID"); result.setInvalidRows(result.getInvalidRows() + 1); }
            else if (!row.getConflicts().isEmpty()) { row.setStatus("CONFLICT"); result.setConflictRows(result.getConflictRows() + 1); }
            else if (row.getColor().isEmpty() || row.getSize().isEmpty()) { row.setStatus("NEEDS_SPEC"); result.setNeedsSpecRows(result.getNeedsSpecRows() + 1); }
            else { row.setStatus("READY"); result.setReadyRows(result.getReadyRows() + 1); }
        }
    }

    private String normalize(String value) { return ProductCatalogService.text(value).toLowerCase(Locale.ROOT); }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
