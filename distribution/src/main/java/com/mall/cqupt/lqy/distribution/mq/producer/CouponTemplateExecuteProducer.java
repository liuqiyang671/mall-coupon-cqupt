package com.mall.cqupt.lqy.distribution.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.mall.cqupt.lqy.distribution.common.constant.DistributionRocketMQConstant;
import com.mall.cqupt.lqy.distribution.mq.base.BaseSendExtendDTO;
import com.mall.cqupt.lqy.distribution.mq.base.MessageWrapper;
import com.mall.cqupt.lqy.distribution.mq.event.CouponTemplateExecuteEvent;
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
 * 优惠券推送任务执行生产者
 */
@Slf4j
@Component
public class CouponTemplateExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTemplateExecuteEvent> {

    private final ConfigurableEnvironment environment;

    public CouponTemplateExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTemplateExecuteEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券发放执行")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic(environment.resolvePlaceholders(DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateExecuteEvent event, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), event))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
