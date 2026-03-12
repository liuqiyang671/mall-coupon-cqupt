package com.mall.cqupt.merchant.admin.common.constant;

/**
 * 商家后管优惠券 RocketMQ 常量类
 */
public final class MerchantAdminRocketMQConstant {

    /**
     * 优惠券模板推送定时执行 Topic Key
     */
    public static final String TEMPLATE_TASK_DELAY_TOPIC_KEY = "one-coupon_merchant-admin-service_coupon-task-delay_topic${unique-name:}";

    /**
     * 优惠券模板推送定时执行-变更记录发送状态消费者组 Key
     */
    public static final String TEMPLATE_TASK_DELAY_STATUS_CG_KEY = "one-coupon_merchant-admin-service_coupon-task-delay-status_cg${unique-name:}";

    /**
     * 优惠券模板推送执行 Topic Key
     * 负责扫描优惠券 Excel 并将里面的记录进行推送
     */
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "one-coupon_distribution-service_coupon-task-execute_topic${unique-name:}";
}

