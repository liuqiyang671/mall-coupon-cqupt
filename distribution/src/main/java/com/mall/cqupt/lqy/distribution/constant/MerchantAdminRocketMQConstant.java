package com.mall.cqupt.lqy.distribution.constant;

/**
 * 商家后管优惠券 RocketMQ 常量类
 */
public final class MerchantAdminRocketMQConstant {

    /**
     * 优惠券模板推送定时执行 Topic Key
     */
    public static final String TEMPLATE_TASK_DELAY_TOPIC_KEY = "one-coupon_merchant-admin-service_coupon-task-delay_topic${unique-name:}";

    /**
     * 优惠券模板推送定时执行-执行发送开始执行指令消费者组 Key
     */
    public static final String TEMPLATE_TASK_SEND_EXECUTE_CG_KEY = "one-coupon_merchant-admin-service_coupon-task-send-execute_cg${unique-name:}";
}
