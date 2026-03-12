package com.mall.cqupt.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户优惠券状态枚举
 */
@RequiredArgsConstructor
public enum UserCouponStatusEnum {

    /**
     * 未使用
     */
    UNUSED(0),

    /**
     * 已使用
     */
    USED(1),

    /**
     * 已过期
     */
    EXPIRED(2),

    /**
     * 已撤回
     */
    REVOKED(3);

    @Getter
    private final int code;
}
