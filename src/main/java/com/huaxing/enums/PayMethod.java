package com.huaxing.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PayMethod {
    CASH, WECHAT, ALIPAY;

    @EnumValue
    private final String value;

    PayMethod() {
        this.value = name();
    }
}
