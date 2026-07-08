package com.huaxing.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {
    BOSS, CLERK;

    @EnumValue
    private final String value;

    UserRole() {
        this.value = name();
    }
}
