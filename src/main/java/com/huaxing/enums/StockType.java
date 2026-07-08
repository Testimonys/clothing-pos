package com.huaxing.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum StockType {
    INBOUND, OUTBOUND;

    @EnumValue
    private final String value;

    StockType() {
        this.value = name();
    }
}
