package com.mall.cqupt.merchant.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    NORMAL(0, "正常"),
    DISABLED(1, "禁用");

    private final Integer status;
    private final String description;
}
