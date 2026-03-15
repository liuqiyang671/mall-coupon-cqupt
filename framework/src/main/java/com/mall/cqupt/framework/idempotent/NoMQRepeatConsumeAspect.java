package com.mall.cqupt.framework.idempotent;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 防止消息队列消费者重复消费消息切面控制器
 */
@Aspect
@RequiredArgsConstructor
public final class NoMQRepeatConsumeAspect {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 增强方法标记 {@link NoMQRepeatConsume} 注解逻辑
     */
    @Around("@annotation(com.mall.cqupt.framework.idempotent.NoMQRepeatConsume)")
    public Object noMQRepeatConsume(ProceedingJoinPoint joinPoint) throws Throwable {
        NoMQRepeatConsume noMQRepeatConsume = getNoMQRepeatConsumeAnnotation(joinPoint);
        String uniqueKey = (String) SpELUtil.parseKey(noMQRepeatConsume.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        Boolean setIfAbsent = stringRedisTemplate.opsForValue()
                .setIfAbsent(uniqueKey, StrUtil.EMPTY, noMQRepeatConsume.keyTimeout(), TimeUnit.SECONDS);
        // 设置失败意味着已经有这个 Key，我们直接返回空即可
        if (!setIfAbsent) {
            return null;
        }
        Object result;
        try {
            // 执行标记了消息队列防重复消费注解的方法原逻辑
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 删除幂等 Key，让消息队列消费者重试逻辑进行重新消费
            stringRedisTemplate.delete(uniqueKey);
            throw ex;
        }
        return result;
    }

    /**
     * @return 返回自定义防重复消费注解
     */
    public static NoMQRepeatConsume getNoMQRepeatConsumeAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(NoMQRepeatConsume.class);
    }
}
