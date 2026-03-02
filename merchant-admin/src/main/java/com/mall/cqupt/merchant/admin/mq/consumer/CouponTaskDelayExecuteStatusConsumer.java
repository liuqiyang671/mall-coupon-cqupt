package com.mall.cqupt.merchant.admin.mq.consumer;

import com.alibaba.fastjson2.JSON;

import com.mall.cqupt.merchant.admin.common.constant.MerchantAdminRocketMQConstant;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTaskDO;
import com.mall.cqupt.merchant.admin.mq.base.MessageWrapper;
import com.mall.cqupt.merchant.admin.mq.event.CouponTaskDelayEvent;
import com.mall.cqupt.merchant.admin.service.CouponTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 优惠券推送延迟执行-变更记录发送状态消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MerchantAdminRocketMQConstant.TEMPLATE_TASK_DELAY_TOPIC_KEY,   // 订阅的话题，即从哪个“频道”拿消息。
        consumerGroup = MerchantAdminRocketMQConstant.TEMPLATE_TASK_DELAY_STATUS_CG_KEY  //消费者组，同一组内的消费者共同分担该 Topic 的消息。
)
@Slf4j(topic = "CouponTaskDelayExecuteStatusConsumer") //通过 topic 参数指定了日志名称，方便在日志文件中进行过滤检索。
public class CouponTaskDelayExecuteStatusConsumer implements RocketMQListener<MessageWrapper<CouponTaskDelayEvent>> {

    private final CouponTaskService couponTaskService;

    @Override
    public void onMessage(MessageWrapper<CouponTaskDelayEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 优惠券推送定时执行@变更记录发送状态 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        // 修改延时执行推送任务任务状态为执行中
        CouponTaskDelayEvent message = messageWrapper.getMessage();
        CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                .id(message.getCouponTaskId())
                .status(message.getStatus())
                .build();
        couponTaskService.updateById(couponTaskDO);
    }
}
