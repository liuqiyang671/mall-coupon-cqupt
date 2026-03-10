package com.mall.cqupt.engine.common.constant;

/**
 * 分布式 Redis 缓存引擎层常量类
 */
public final class EngineRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "one-coupon_engine:template:%s";

    /**
     * 优惠券模板缓存分布式锁 Key
     */
    public static final String LOCK_COUPON_TEMPLATE_KEY = "one-coupon_engine:lock:template:%s";

    /**
     * 优惠券模板缓存 Key
     */
    public static final String USER_COUPON_TEMPLATE_LIMIT_KEY = "one-coupon_engine:user-template-limit:%s_%s";

    /**
     * 优惠券模板缓存空值 Key
     */
    public static final String COUPON_TEMPLATE_IS_NULL_KEY = "one-coupon_engine:template_is_null:%s";

}