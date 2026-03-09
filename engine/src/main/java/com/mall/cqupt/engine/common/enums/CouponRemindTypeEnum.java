package com.mall.cqupt.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预约提醒方式枚举类
 */
@RequiredArgsConstructor
public enum CouponRemindTypeEnum {

    /**
     * 邮件提醒
     */
    EMAIL(0);

    @Getter
    private final int type;
}
