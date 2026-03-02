package com.mall.cqupt.merchant.admin.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.mall.cqupt.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTaskDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTaskMapper;
import com.mall.cqupt.merchant.admin.mq.event.CouponTaskDelayEvent;
import com.mall.cqupt.merchant.admin.mq.producer.CouponTaskDelayExecuteProducer;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 优惠券推送任务扫描定时发送记录 XXL-Job 处理器
 */
@Component
@RequiredArgsConstructor
public class CouponTaskJobHandler extends IJobHandler {

    private final CouponTaskMapper couponTaskMapper;
    /**
     * 优惠券任务延迟执行消息生产者
     * 负责将优惠券任务转换为消息事件发送到消息队列
     */
    private final CouponTaskDelayExecuteProducer couponTaskDelayExecuteProducer;
    /**
     * 单次查询最大限制数量
     * 控制每次批量处理的数据量，避免内存溢出和数据库压力过大
     */
    private static final int MAX_LIMIT = 100;

    @XxlJob(value = "couponTemplateTask")
    public void execute() throws Exception {
        // 初始化游标ID和当前时间
        long initId = 0;
        Date now = new Date();

        // 滚动查询循环，直到没有更多待处理任务
        while (true) {
            List<CouponTaskDO> couponTaskDOList = fetchPendingTasks(initId, now);

            if (CollUtil.isEmpty(couponTaskDOList)) {
                break;
            }

            // 调用分发服务对用户发送优惠券
            for (CouponTaskDO each : couponTaskDOList) {
                distributeCoupon(each);
            }

            if (couponTaskDOList.size() < MAX_LIMIT) {
                break;
            }

            // 更新 initId 为当前列表中最大 ID
            initId = couponTaskDOList.stream()
                    .mapToLong(CouponTaskDO::getId)
                    .max()
                    .orElse(initId);
        }
    }

    private void distributeCoupon(CouponTaskDO couponTask) {
        // 通过消息队列发送消息，修改状态记录并由分发服务消费者消费该消息
        CouponTaskDelayEvent couponTaskDelayEvent = CouponTaskDelayEvent.builder()
                .couponTaskId(couponTask.getId())
                .status(CouponTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        couponTaskDelayExecuteProducer.sendMessage(couponTaskDelayEvent);
    }

    private List<CouponTaskDO> fetchPendingTasks(long initId, Date now) {
        LambdaQueryWrapper<CouponTaskDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskDO.class)
                .eq(CouponTaskDO::getStatus, CouponTaskStatusEnum.PENDING.getStatus()) // 1. 状态必须是“待执行”
                .le(CouponTaskDO::getSendTime, now)    // 2. 发送时间 <= 当前时间（到了发送时间，或者已经超时了都要发）
                .gt(CouponTaskDO::getId, initId)       // 3. ID > 上次的最大ID（滚动游标）
                .last("LIMIT " + MAX_LIMIT);    // 4. 每次只取 100 条
        return couponTaskMapper.selectList(queryWrapper);
    }
}
