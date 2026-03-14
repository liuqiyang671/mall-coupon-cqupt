package com.mall.cqupt.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 取消预约提醒的布隆过滤器属性配置
 */
@Data
@Component
@ConfigurationProperties(prefix = CancelRemindBloomFilterProperties.PREFIX)
public class CancelRemindBloomFilterProperties {

    public static final String PREFIX = "engine.cache.redis.bloom-filter.cancel-remind";

    /**
     * 用户注册布隆过滤器实例名称
     */
    private String name = "cancel_remind_cache_penetration_bloom_filter";

    /**
     * 预期插入量
     */
    private Long expectedInsertions = 6400L;

    /**
     * 预期错误概率
     */
    private Double falseProbability = 0.03D;

}
