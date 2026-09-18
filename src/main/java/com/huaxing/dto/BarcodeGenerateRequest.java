package com.huaxing.dto;

import lombok.Data;

/** luohuai codeX generate: all business segments required for a 16-digit system barcode. */
@Data
public class BarcodeGenerateRequest {
    private Long dealerId;
    private String productCode;
    private Long sizeConfigId;
    private Long colorConfigId;
}
