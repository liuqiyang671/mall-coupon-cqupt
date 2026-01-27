package com.mall.cqupt.lqy.distribution.common.constant;

/**
 * 分发优惠券服务 RocketMQ 常量类
 */
public final class DistributionRocketMQConstant {

    /**
     * 优惠券模板推送执行 Topic Key
     */
    public static final String TEMPLATE_TASK_EXECUTE_TOPIC_KEY = "one-coupon_distribution-service_coupon-task-execute_topic${unique-name:}";

    /**
     * 优惠券模板推送执行-执行消费者组 Key
     */
    public static final String TEMPLATE_TASK_EXECUTE_CG_KEY = "one-coupon_distribution-service_coupon-task-execute_cg${unique-name:}";
}
