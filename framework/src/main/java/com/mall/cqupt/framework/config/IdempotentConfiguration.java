package com.mall.cqupt.framework.config;


import com.mall.cqupt.framework.idempotent.NoMQRepeatConsumeAspect;
import com.mall.cqupt.framework.idempotent.NoRepeatSubmitAspect;
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
    public NoRepeatSubmitAspect noRepeatSubmitAspect(RedissonClient redissonClient) {
        return new NoRepeatSubmitAspect(redissonClient);
    }

    /**
     * 防止消息队列消费者重复消费消息切面控制器
     */
    @Bean
    public NoMQRepeatConsumeAspect noMQRepeatConsumeAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoMQRepeatConsumeAspect(stringRedisTemplate);
    }
}
