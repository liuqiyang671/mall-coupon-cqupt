package com.mall.cqupt.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;

import com.mall.cqupt.merchant.admin.common.constant.MerchantAdminRocketMQConstant;
import com.mall.cqupt.merchant.admin.mq.base.BaseSendExtendDTO;
import com.mall.cqupt.merchant.admin.mq.base.MessageWrapper;
import com.mall.cqupt.merchant.admin.mq.event.CouponTaskDelayEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 优惠券推送任务定时执行生产者
 */
@Slf4j
@Component
public class CouponTaskDelayExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTaskDelayEvent> {

    private final ConfigurableEnvironment environment;

    public CouponTaskDelayExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTaskDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券推送定时执行")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic(environment.resolvePlaceholders(MerchantAdminRocketMQConstant.TEMPLATE_TASK_DELAY_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTaskDelayEvent couponTaskDelayEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), couponTaskDelayEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
