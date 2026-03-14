package com.mall.cqupt.engine.mq.producer;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import com.mall.cqupt.engine.common.constant.EngineRockerMQConstant;
import com.mall.cqupt.engine.mq.base.BaseSendExtendDTO;
import com.mall.cqupt.engine.mq.base.MessageWrapper;
import com.mall.cqupt.engine.mq.event.CouponRemindEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 提醒抢券生产者
 */
@Slf4j
@Component
public class CouponRemindProducer extends AbstractCommonSendProduceTemplate<CouponRemindEvent> {

    private final ConfigurableEnvironment environment;

    public CouponRemindProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponRemindEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("提醒用户抢券")
                .keys(messageSendEvent.getUserId() + ":" + messageSendEvent.getCouponTemplateId())
                .topic(environment.resolvePlaceholders(EngineRockerMQConstant.COUPON_TEMPLATE_REMIND_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponRemindEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }

    @Override
    public SendResult sendMessage(CouponRemindEvent messageSendEvent) {
        // 提醒时间
        DateTime remindTime = DateUtil.offsetMinute(messageSendEvent.getStartTime(), -messageSendEvent.getRemindTime());
        return sendMessage(messageSendEvent, remindTime.getTime());
    }
}
