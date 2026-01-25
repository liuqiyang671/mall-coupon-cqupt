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

    //用于访问应用程序的配置属性，如 application.properties 或 application.yml 中的配置
    private final ConfigurableEnvironment environment;

    /**
     * 构造函数，注入依赖项
     *
     * @param rocketMQTemplate RocketMQ模板实例，用于发送消息
     * @param environment Spring环境配置实例，用于解析配置项
     */
    public CouponTaskDelayExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTaskDelayEvent messageSendEvent) {
        // 使用建造者模式构建基础发送参数对象
        return BaseSendExtendDTO.builder()
                .eventName("优惠券推送定时执行")  // 设置事件名称，用于日志记录和识别
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))  // 设置消息键值，使用任务ID转换为字符串
                .topic(environment.resolvePlaceholders(MerchantAdminRocketMQConstant.TEMPLATE_TASK_DELAY_TOPIC_KEY))  // 设置主题，解析配置中的占位符
                .sentTimeout(2000L)  // 设置发送超时时间为2秒
                .build();  // 构建最终对象
    }

    @Override
    protected Message<?> buildMessage(CouponTaskDelayEvent couponTaskDelayEvent, BaseSendExtendDTO requestParam) {
        // 如果请求参数中的键值为空，则生成新的UUID作为键值，否则使用原有键值
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();

        // 使用Spring消息构建器创建消息对象
        return MessageBuilder
                // 设置消息载荷，使用MessageWrapper包装实际的优惠券任务延迟事件
                .withPayload(new MessageWrapper(requestParam.getKeys(), couponTaskDelayEvent))
                // 设置消息键值头部，用于消息追踪和去重
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息标签头部
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // 构建最终的消息对象
                .build();
    }
}
