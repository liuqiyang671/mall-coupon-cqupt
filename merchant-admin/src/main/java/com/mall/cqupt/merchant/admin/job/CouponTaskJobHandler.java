package com.mall.cqupt.merchant.admin.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTaskDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTaskMapper;
import com.mall.cqupt.merchant.admin.mq.event.CouponTaskDelayEvent;
import com.mall.cqupt.merchant.admin.mq.producer.CouponTaskDelayExecuteProducer;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 优惠券推送任务扫描定时发送记录 XXL-Job 处理器
 * <p>
 *     定时扫描数据库，找出那些“已经到了发送时间”却“还没开始发送”的优惠券任务，并把它们投递到消息队列中执行。
 */
@Component
@RequiredArgsConstructor
@RestController // 为了保障快速启动，可通过 Swagger 方式访问接口，可以减少一个中间件 XXL-Job
@Tag(name = "优惠券定时推送任务") // 为了保障快速启动，可通过 Swagger 方式访问接口，可以减少一个中间件 XXL-Job
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

    @SneakyThrows
    @Operation(summary = "执行优惠券定时推送") // 为了保障快速启动，可通过 Swagger 方式访问接口，可以减少一个中间件 XXL-Job
    @GetMapping("/api/merchant-admin/other/coupon-task/job") // 为了保障快速启动，可通过 Swagger 方式访问接口，可以减少一个中间件 XXL-Job
    public Result<Void> webExecute() {
        execute();
        return Results.success();
    }

    @XxlJob(value = "couponTemplateTask")
    public void execute() throws Exception {
        // 初始化游标ID和当前时间
        long initId = 0;
        Date now = new Date();

        // 滚动查询循环，直到没有更多待处理任务
        while (true) {
            // 分批次获取待处理的任务
            List<CouponTaskDO> couponTaskDOList = fetchPendingTasks(initId, now);

            // 如果查不到数据了，说明全部处理完毕，跳出循环
            if (CollUtil.isEmpty(couponTaskDOList)) {
                break;
            }

            // 调用分发服务对用户发送优惠券
            for (CouponTaskDO each : couponTaskDOList) {
                distributeCoupon(each);
            }

            // 如果查出来的数量小于 100，说明这是最后一页了，处理完直接退出
            if (couponTaskDOList.size() < MAX_LIMIT) {
                break;
            }

            // 更新 initId 为当前列表中最大 ID  下一次循环查询时，会从这个 ID 之后继续往后找，避免重复查询
            initId = couponTaskDOList.stream()
                    .mapToLong(CouponTaskDO::getId)
                    .max()
                    .orElse(initId);
        }
    }

    private void distributeCoupon(CouponTaskDO couponTask) {
        // 通过消息队列发送消息，修改状态记录并由分发服务消费者消费该消息
        // 构造一个消息协议对象（Event）
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
