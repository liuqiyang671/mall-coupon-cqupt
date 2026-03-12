package com.mall.cqupt.engine.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Configuration
public class RBloomFilterConfiguration {

    /**
     * 优惠券查询缓存穿透布隆过滤器
     */
    @Bean
    public RBloomFilter<String> couponTemplateQueryBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("couponTemplateQueryBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }

    /**
     * 防止取消提醒缓存穿透的布隆过滤器
     */
    @Bean
    public RBloomFilter<String> cancelRemindBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("cancelRemindBloomFilter");
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }
}
