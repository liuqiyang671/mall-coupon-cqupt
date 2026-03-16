package com.mall.cqupt.framework.config;


import com.mall.cqupt.framework.idempotent.NoMQDuplicateConsumeAspect;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 防重复提交组件配置类
 */
public class IdempotentConfiguration {

    /**
     * 防止用户重复提交表单信息切面控制器
     */
    @Bean
    public NoDuplicateSubmitAspect noRepeatSubmitAspect(RedissonClient redissonClient) {
        return new NoDuplicateSubmitAspect(redissonClient);
    }

    /**
     * 防止消息队列消费者重复消费消息切面控制器
     */
    @Bean
    public NoMQDuplicateConsumeAspect noMQRepeatConsumeAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoMQDuplicateConsumeAspect(stringRedisTemplate);
    }
}
