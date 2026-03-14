package com.mall.cqupt.engine.service.handler.remind;



import cn.hutool.json.JSONUtil;
import com.mall.cqupt.engine.common.enums.CouponRemindTypeEnum;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import com.mall.cqupt.engine.service.handler.remind.impl.SendEmailRemindCouponTemplate;
import com.mall.cqupt.engine.service.handler.remind.impl.SendMessageRemindCouponTemplate;
import lombok.AllArgsConstructor;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.mall.cqupt.engine.common.constant.EngineRedisConstant.COUPON_REMIND_CHECK_KEY;


/**
 * 执行相应的抢券提醒
 */
@Component
@AllArgsConstructor
public class ExecuteRemindCouponTemplate {

    // 提醒用户属于IO密集型任务
    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    public static final String REDIS_BLOCKING_DEQUE = "COUPON_REMIND_QUEUE";
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateRemindService couponTemplateRemindService;
    private final SendEmailRemindCouponTemplate sendEmailRemindCouponTemplate;
    private final SendMessageRemindCouponTemplate sendMessageRemindCouponTemplate;

    /**
     * 执行提醒
     *
     * @param remindDTO 需要的信息
     */
    public void executeRemindCouponTemplate(RemindCouponTemplateDTO remindDTO) {
        executorService.execute(() -> {
            if (!couponTemplateRemindService.isCancelRemind(remindDTO)) {
                // 用户没取消预约，则发出提醒
                // 假设刚把消息提交到线程池，突然应用宕机了，我们通过延迟队列进行兜底 Refresh
                RBlockingDeque<String> blockingDeque = redissonClient.getBlockingDeque(REDIS_BLOCKING_DEQUE);
                RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
                String key = String.format(COUPON_REMIND_CHECK_KEY, remindDTO.getUserId(), remindDTO.getCouponTemplateId(), remindDTO.getRemindTime(), remindDTO.getType());
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(remindDTO));
                delayedQueue.offer(key, 5, TimeUnit.SECONDS);
                // 提醒用户
                switch (Objects.requireNonNull(CouponRemindTypeEnum.getByType(remindDTO.getType()))) {
                    case EMAIL -> sendEmailRemindCouponTemplate.remind(remindDTO);
                    case MESSAGE -> sendMessageRemindCouponTemplate.remind(remindDTO);
                    default -> {
                    }
                }
                // 提醒完后删除key
                stringRedisTemplate.delete(key);
            }
        });
    }
}
