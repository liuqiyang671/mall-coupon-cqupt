package com.mall.cqupt.merchant.admin.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserActivationStatusEnum {

    INACTIVE(0, "未激活"),
    ACTIVE(1, "已激活");

    private final Integer status;
    private final String description;
}
