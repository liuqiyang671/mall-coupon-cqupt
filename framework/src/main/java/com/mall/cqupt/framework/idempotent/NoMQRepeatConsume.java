package com.mall.cqupt.framework.idempotent;

import java.lang.annotation.*;

/**
 * 幂等注解，防止消息队列消费者重复消费消息
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoMQRepeatConsume {

    /**
     * 通过 SpEL 表达式生成的唯一 Key
     */
    String key();

    /**
     * 设置防重令牌 Key 过期时间，单位秒，默认 1 小时
     */
    long keyTimeout() default 3600L;
}
