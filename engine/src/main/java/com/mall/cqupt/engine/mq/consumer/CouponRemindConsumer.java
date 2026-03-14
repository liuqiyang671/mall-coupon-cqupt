package com.mall.cqupt.engine.mq.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;

import com.mall.cqupt.engine.common.constant.EngineRockerMQConstant;
import com.mall.cqupt.engine.mq.base.MessageWrapper;
import com.mall.cqupt.engine.mq.event.CouponRemindEvent;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import com.mall.cqupt.engine.service.handler.remind.ExecuteRemindCouponTemplate;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 提醒抢券消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = EngineRockerMQConstant.COUPON_TEMPLATE_REMIND_TOPIC_KEY,
        consumerGroup = EngineRockerMQConstant.COUPON_TEMPLATE_REMIND_CG_KEY
)
@Slf4j(topic = "CouponRemindConsumer")
public class CouponRemindConsumer implements RocketMQListener<MessageWrapper<CouponRemindEvent>> {

    private final ExecuteRemindCouponTemplate executeRemindCouponTemplate;
    private final CouponTemplateRemindService couponTemplateRemindService;

    @Override
    public void onMessage(MessageWrapper<CouponRemindEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 提醒用户抢券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));
        CouponRemindEvent event = messageWrapper.getMessage();
        RemindCouponTemplateDTO remindCouponTemplateDTO = BeanUtil.toBean(event, RemindCouponTemplateDTO.class);
        // 提醒用户
        executeRemindCouponTemplate.executeRemindCouponTemplate(remindCouponTemplateDTO);
    }
}
