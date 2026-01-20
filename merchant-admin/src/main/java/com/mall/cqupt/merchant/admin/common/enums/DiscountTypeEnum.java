package com.mall.cqupt.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券优惠类型
 */
@RequiredArgsConstructor
public enum DiscountTypeEnum {

    /**
     * 立减券
     */
    FIXED_DISCOUNT(0),

    /**
     * 满减券
     */
    THRESHOLD_DISCOUNT(1),

    /**
     * 折扣券
     */
    DISCOUNT_COUPON(2);

    @Getter
    private final int type;
}
