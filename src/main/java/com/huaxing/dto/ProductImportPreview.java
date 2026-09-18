package com.huaxing.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** luohuai codeX generate: a read-only preview reports source rows and unresolved specifications, never stock. */
@Data
public class ProductImportPreview {
    // luohuai codeX generate: file-level dealer provenance applies to every imported product row.
    private Long dealerId;
    private String dealerCode;
    private String dealerName;
    private int totalRows;
    private int productCount;
    private int readyRows;
    private int needsSpecRows;
    private int conflictRows;
    private int invalidRows;
    private final List<String> notes = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    @Data
    public static class Row {
        private String sheet;
        private int sourceRow;
        private String productCode;
        private String name;
        private String unit;
        private String barcode;
        private String color;
        private String size;
        private String categoryName;
        private BigDecimal costPrice;
        private BigDecimal sellPrice;
        private String status;
        private final List<String> errors = new ArrayList<>();
        private final List<String> conflicts = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
    }
}
