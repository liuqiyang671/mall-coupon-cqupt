package com.cqupt.settlement.common.constant;

/**
 * 结算模块 Redis Key 常量
 */
public final class SettlementRedisConstant {

    public static final String COUPON_TEMPLATE_KEY = "one-coupon_engine:template:%s";

    public static final String USER_COUPON_TEMPLATE_LIST_KEY = "one-coupon_engine:user-template-list:%s";

    private SettlementRedisConstant() {
    }
}
