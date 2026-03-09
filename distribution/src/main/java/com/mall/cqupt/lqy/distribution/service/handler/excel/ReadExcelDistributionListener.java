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
    private int rowCount = 0; // 当前读取到了Excel的第几行
    private final static String STOCK_DECREMENT_USER_RECORD_LUA_PATH = "lua/stock_decrement_user_record.lua";

    // 逐行处理 Excel 数据。 Excel 里有多少行用户数据，这个方法就会被触发多少次。
    @Override
    public void invoke(CouponTaskExcelObject data, AnalysisContext context) {
        ++rowCount;
        String couponTaskId = String.valueOf(couponTask.getId());

        // 获取当前进度，判断是否已经执行过。如果已执行，则跳过即可，防止执行到一半应用宕机
        String templateTaskExecuteProgressKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY, couponTaskId);
        String progress = stringRedisTemplate.opsForValue().get(templateTaskExecuteProgressKey);
        // 如果进度存在，并且 Redis 记录的行数 > 当前行数，则把当前行号对齐到历史进度，该历史进度为之前已发过卷的人数，所以需要return跳过后续逻辑
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) > rowCount) {
            rowCount = Integer.parseInt(progress);
            return;
        }

        // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
        // 执行 Lua 脚本原子扣减库存（防超卖核心机制）
        // 单例模式：保证在整个程序的运行期间，某个对象在内存中永远只被创建一次，有且只有这一个实例供大家共享使用。
        // 参数1 (Key)：使用脚本的路径作为唯一标识（例如 "lua/stock.lua"）。
        // 参数2 (Supplier 函数)：一段“对象制造说明书”。只有当内存池里找不到这个 Key 对应的对象时，才会执行这段括号里的逻辑去真正加载文件。
        DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_USER_RECORD_LUA_PATH, () -> {
            // 1. 创建 Spring Data Redis 提供的 Lua 脚本操作对象
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            // 2. 指定 Lua 脚本的物理文件来源
            // ClassPathResource: 告诉 Spring 去项目的类路径（通常是 src/main/resources 目录）下找这个文件
            // ResourceScriptSource: 将找到的物理文件包装成 Spring Redis 能够识别的脚本资源流
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_USER_RECORD_LUA_PATH)));
            // 3. 强制设置脚本的返回值类型
            // 必须显式声明！否则 Redis 默认返回的可能是反序列化不了的字节序列，导致代码抛出 ClassCastException（类型转换异常）
            redisScript.setResultType(Long.class);
            // 4. 返回完整装配好的脚本对象
            // 这个对象会被 Hutool 的 Singleton 内部缓存起来（存入类似 ConcurrentHashMap 的结构中）。
            // 下次 EasyExcel 读到下一行数据再次执行这行代码时，会直接从缓存拿，不再执行上述 1~4 步。
            return redisScript;
        });

        // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录
        String couponTemplateKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, couponTemplate.getId());
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, couponTaskId);
        Long combinedField = stringRedisTemplate.execute(buildLuaScript, ListUtil.of(couponTemplateKey, batchUserSetKey), data.getUserId());

        // firstField 为 false 说明优惠券已经没有库存了  解析 Lua 返回结果并兜底库存耗尽场景
        boolean firstField = StockDecrementReturnCombinedUtil.extractFirstField(combinedField);
        if (!firstField) {
            // 同步当前执行进度到缓存
            stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
            // TODO 应该添加到 t_coupon_task_fail 并标记错误原因
            return;
        }

        // 为什么是 >= 而不是 = BATCH_USER_COUPON_SIZE？
        // 考虑到有可能执行到这一步应用会宕机。假设当 batchUserSet 已经 5000 条了，消费者消费到这里宕机，当再执行 LUA 脚本到这一步时，BATCH_USER_COUPON_SIZE 已经 5001
        long batchUserSetSize = StockDecrementReturnCombinedUtil.extractSecondField(combinedField);

        // 为了避免数据库压力过大，这里通过消息队列进行削峰,解析当前发送批次并投递 MQ 进行异步削峰
        CouponTemplateExecuteEvent couponTemplateExecuteEvent = CouponTemplateExecuteEvent.builder()
                .userId(data.getUserId())
                .mail(data.getMail())
                .phone(data.getPhone())
                .couponTaskId(couponTaskId)
                .notifyType(couponTask.getNotifyType())
                .shopNumber(couponTask.getShopNumber())
                .couponTemplateId(couponTemplate.getId())
                .couponTemplateConsumeRule(couponTemplate.getConsumeRule())
                .batchUserSetSize(batchUserSetSize)
                .distributionEndFlag(Boolean.FALSE)
                .build();
        couponTemplateExecuteProducer.sendMessage(couponTemplateExecuteEvent);

        // 同步当前执行进度到缓存
        stringRedisTemplate.opsForValue().set(templateTaskExecuteProgressKey, String.valueOf(rowCount));
    }

    // 收尾工作。当 Excel 的最后一行被读取完毕后，触发此方法。
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 发送 Excel 解析完成标识，即使不满足批量保存的数量也得保存到数据库
        CouponTemplateExecuteEvent couponTemplateExecuteEvent = CouponTemplateExecuteEvent.builder()
                .batchUserSetSize(-1L)
                .distributionEndFlag(Boolean.TRUE) // 设置解析完成标识
                .shopNumber(couponTask.getShopNumber())
                .couponTemplateId(couponTemplate.getId())
                .couponTemplateConsumeRule(couponTemplate.getConsumeRule())
                .couponTaskId(String.valueOf(couponTask.getId()))
                .build();
        couponTemplateExecuteProducer.sendMessage(couponTemplateExecuteEvent);
    }
}


