package com.mall.cqupt.merchant.admin.common.constant;

public final class MerchantAdminRedisConstant {

    public static final String COUPON_TEMPLATE_KEY = "one-coupon_engine:template:%s";

    public static final String USER_LOGIN_FAIL_COUNT_KEY = "one-coupon_merchant-admin:user:login-fail:%s:%s";

    public static final String USER_LOGIN_LOCK_KEY = "one-coupon_merchant-admin:user:login-lock:%s:%s";

    public static final String USER_TOKEN_KEY = "one-coupon_merchant-admin:user:token:%s:%s";
}
