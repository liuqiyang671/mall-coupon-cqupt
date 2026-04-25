package com.mall.cqupt.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GoodsStatusEnum {

    OFF_SHELF(0, "下架"),
    ON_SHELF(1, "上架"),
    VIOLATION(2, "违规下架");

    @Getter
    private final int status;

    @Getter
    private final String description;
}
