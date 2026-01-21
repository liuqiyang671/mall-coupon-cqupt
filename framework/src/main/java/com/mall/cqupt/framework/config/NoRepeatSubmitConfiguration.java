package com.mall.cqupt.framework.config;


import com.mall.cqupt.framework.idempotent.NoRepeatSubmitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;

/**
 * 防重复提交组件配置类
 */
public class NoRepeatSubmitConfiguration {

    /**
     * 防止用户重复提交表单信息切面控制器
     */
    @Bean
    public NoRepeatSubmitAspect noRepeatSubmitAspect(RedissonClient redissonClient) {
        return new NoRepeatSubmitAspect(redissonClient);
    }
}
