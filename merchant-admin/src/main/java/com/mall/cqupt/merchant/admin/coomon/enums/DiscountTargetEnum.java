package com.mall.cqupt.merchant.admin.coomon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 优惠券优惠对象枚举
 */
@RequiredArgsConstructor
public enum DiscountTargetEnum {

    /**
     * 商品专属优惠
     */
    PRODUCT_SPECIFIC(0),
    /**
     * 全店通用优惠
     */
    ALL_STORE_GENERAL(1);

    @Getter
    private final int type;
}
