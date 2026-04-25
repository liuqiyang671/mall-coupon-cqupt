package com.mall.cqupt.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GoodsCategoryStatusEnum {

    ENABLED(0, "启用"),
    DISABLED(1, "禁用");

    @Getter
    private final int status;

    @Getter
    private final String description;
}
