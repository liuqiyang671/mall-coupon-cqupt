package com.mall.cqupt.engine.common.constant;

/**
 * 优惠券引擎层服务 RocketMQ 常量类
 */
public final class EngineRockerMQConstant {

    /**
     * 用户优惠券到期后关闭 Topic Key
     */
    public static final String USER_COUPON_DELAY_CLOSE_TOPIC_KEY = "one-coupon_engine-service_user-coupon-delay-close_topic${unique-name:}";

    /**
     * 用户优惠券到期后关闭消费者组 Key
     */
    public static final String USER_COUPON_DELAY_CLOSE_CG_KEY = "one-coupon_engine-service_user-coupon-delay-close_cg${unique-name:}";
}
