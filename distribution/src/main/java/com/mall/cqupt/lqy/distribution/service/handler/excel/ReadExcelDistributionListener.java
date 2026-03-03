package com.mall.cqupt.lqy.distribution.service.handler.excel;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import com.mall.cqupt.lqy.distribution.common.constant.DistributionRedisConstant;
import com.mall.cqupt.lqy.distribution.common.constant.EngineRedisConstant;
import com.mall.cqupt.lqy.distribution.dao.entity.CouponTaskDO;
import com.mall.cqupt.lqy.distribution.mq.event.CouponTemplateExecuteEvent;
import com.mall.cqupt.lqy.distribution.mq.producer.CouponTemplateExecuteProducer;
import com.mall.cqupt.lqy.distribution.remote.dto.resp.CouponTemplateQueryRemoteRespDTO;
import com.mall.cqupt.lqy.distribution.toolkit.StockDecrementReturnCombinedUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * 优惠券任务读取 Excel 分发监听器
 */
@RequiredArgsConstructor
public class ReadExcelDistributionListener extends AnalysisEventListener<CouponTaskExcelObject> {

    private final CouponTaskDO couponTask;
    private final CouponTemplateQueryRemoteRespDTO couponTemplate;

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateExecuteProducer couponTemplateExecuteProducer;

    @Getter
    private int rowCount = 0;
    private final static String STOCK_DECREMENT_USER_RECORD_LUA_PATH = "lua/stock_decrement_user_record.lua";

    @Override
    public void invoke(CouponTaskExcelObject data, AnalysisContext context) {
        ++rowCount;
        String couponTaskId = String.valueOf(couponTask.getId());

        // 获取当前进度，判断是否已经执行过。如果已执行，则跳过即可，防止执行到一半应用宕机
        String templateTaskExecuteProgressKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY, couponTaskId);
        String progress = stringRedisTemplate.opsForValue().get(templateTaskExecuteProgressKey);
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) > rowCount) {
            rowCount = Integer.parseInt(progress);
            return;
        }

        // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
        DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_USER_RECORD_LUA_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_USER_RECORD_LUA_PATH)));
            redisScript.setResultType(Long.class);
            return redisScript;
        });

        // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录
        String couponTemplateKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, couponTemplate.getId());
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, couponTaskId);
        Long combinedFiled = stringRedisTemplate.execute(buildLuaScript, ListUtil.of(couponTemplateKey, batchUserSetKey), data.getUserId());

        // firstField 为 false 说明优惠券已经没有库存了
        boolean firstField = StockDecrementReturnCombinedUtil.extractFirstField(combinedFiled);
        if (!firstField) {
            // 同步当前执行进度到缓存
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
            // TODO 应该添加到 t_coupon_task_fail 并标记错误原因
            return;
        }

        // 为什么是 >= 而不是 = BATCH_USER_COUPON_SIZE？
        // 考虑到有可能执行到这一步应用会宕机。假设当 batchUserSet 已经 5000 条了，消费者消费到这里宕机，当再执行 LUA 脚本到这一步时，BATCH_USER_COUPON_SIZE 已经 5001
        long batchUserSetSize = StockDecrementReturnCombinedUtil.extractSecondField(combinedFiled);

        // 为了避免数据库压力过大，这里通过消息队列进行削峰
        CouponTemplateExecuteEvent couponTemplateExecuteEvent = CouponTemplateExecuteEvent.builder()
                .userId(data.getUserId())
                .mail(data.getMail())
                .phone(data.getPhone())
                .couponTaskId(couponTaskId)
                .couponTemplateId(couponTemplate.getId())
                .couponTemplateConsumeRule(couponTemplate.getConsumeRule())
                .batchUserSetSize(batchUserSetSize)
                .distributionEndFlag(Boolean.FALSE)
                .build();
        couponTemplateExecuteProducer.sendMessage(couponTemplateExecuteEvent);

        // 同步当前执行进度到缓存
        stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 发送 Excel 解析完成标识，即使不满足批量保存的数量也得保存到数据库
        CouponTemplateExecuteEvent couponTemplateExecuteEvent = CouponTemplateExecuteEvent.builder()
                .batchUserSetSize(-1L)
                .distributionEndFlag(Boolean.TRUE) // 设置解析完成标识
                .couponTemplateId(couponTemplate.getId())
                .couponTemplateConsumeRule(couponTemplate.getConsumeRule())
                .couponTaskId(String.valueOf(couponTask.getId()))
                .build();
        couponTemplateExecuteProducer.sendMessage(couponTemplateExecuteEvent);
    }
}

