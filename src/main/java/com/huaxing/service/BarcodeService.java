package com.huaxing.service;

import com.huaxing.dto.BarcodeGenerateRequest;
import com.huaxing.entity.ColorConfig;
import com.huaxing.entity.Dealer;
import com.huaxing.entity.SizeConfig;
import com.huaxing.mapper.BarcodeSequenceMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** luohuai codeX generate: issue explainable 16-digit CODE128 values from stable business codes. */
@Service
public class BarcodeService {
    private final DealerService dealers;
    private final SpecificationConfigService specifications;
    private final BarcodeSequenceMapper sequences;

    public BarcodeService(DealerService dealers, SpecificationConfigService specifications, BarcodeSequenceMapper sequences) {
        this.dealers = dealers;
        this.specifications = specifications;
        this.sequences = sequences;
    }

    @Transactional
    public String generate(BarcodeGenerateRequest request) {
        if (request == null) throw invalid("条码生成参数不能为空");
        Dealer dealer = dealers.requireEnabled(request.getDealerId());
        String productCode = ProductCatalogService.text(request.getProductCode());
        if (!productCode.matches("\\d{1,6}")) throw invalid("生成条码前请填写1-6位数字货号");
        SizeConfig size = specifications.requireSize(request.getSizeConfigId(), true);
        ColorConfig color = specifications.requireColor(request.getColorConfigId(), true);
        String prefix = dealer.getCode() + String.format("%06d", Long.parseLong(productCode)) + size.getCode() + color.getCode();
        if (prefix.length() != 13 || !prefix.matches("\\d{13}")) throw invalid("条码业务编码不完整");

        // luohuai codeX generate: INSERT IGNORE plus row update serializes counters for the same business prefix.
        sequences.ensureExists(prefix);
        if (sequences.increment(prefix) != 1) throw conflict("该经销商、货号、尺码和颜色已生成999个识别码");
        Integer current = sequences.currentValue(prefix);
        if (current == null || current < 1 || current > 999) throw conflict("条码识别码生成失败");
        return prefix + String.format("%03d", current);
    }

    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
