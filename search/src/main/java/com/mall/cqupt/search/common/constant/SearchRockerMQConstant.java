package com.mall.cqupt.search.common.constant;

/**
 * 优惠券搜索层服务 RocketMQ 常量类
 */
public final class SearchRockerMQConstant {

    /**
     * Canal 监听优惠券模板表 Binlog Topic Key
     */
    public static final String TEMPLATE_COUPON_BINLOG_SYNC_TOPIC_KEY = "one-coupon_canal_search-service_es-sync_topic${unique-name:}";

    /**
     * Canal 监听优惠券模板表 Binlog 消费者组 Key
     */
    public static final String TEMPLATE_COUPON_BINLOG_SYNC_CG_KEY = "one-coupon_canal_search-service_es-sync_cg${unique-name:}";

}
