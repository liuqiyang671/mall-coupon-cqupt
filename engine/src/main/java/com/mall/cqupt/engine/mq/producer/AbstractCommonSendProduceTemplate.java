package com.mall.cqupt.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;

import com.mall.cqupt.engine.mq.base.BaseSendExtendDTO;
import com.mall.cqupt.engine.mq.base.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

import java.util.Optional;
import java.util.UUID;

/**
 * RocketMQ 抽象公共发送消息组件
 */
@RequiredArgsConstructor
@Slf4j(topic = "CommonSendProduceTemplate")
public abstract class AbstractCommonSendProduceTemplate<T> {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送事件基础扩充属性实体
     *
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息基本参数，请求头、Keys...
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam     扩充属性实体
     * @return 消息基本参数
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 消息事件通用发送
     *
     * @param messageSendEvent 消息发送事件
     * @return 消息发送返回结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        return sendMessage(messageSendEvent, null);
    }

    /**
     * 消息事件通用发送
     *
     * @param messageSendEvent 消息发送事件
     * @param deliverTimeStamp 任意延迟时间
     * @return 消息发送返回结果
     */
    @SneakyThrows
    public SendResult sendMessage(T messageSendEvent, Long deliverTimeStamp) {
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            if (deliverTimeStamp == null) {
                StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
                if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                    destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
                }

                sendResult = rocketMQTemplate.syncSend(
                        destinationBuilder.toString(),
                        buildMessage(messageSendEvent, baseSendExtendDTO),
                        baseSendExtendDTO.getSentTimeout(),
                        Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0)
                );
            } else {
                byte[] bytes = JSON.toJSONBytes(new MessageWrapper(baseSendExtendDTO.getKeys(), messageSendEvent));
                org.apache.rocketmq.common.message.Message message = new org.apache.rocketmq.common.message.Message(baseSendExtendDTO.getTopic(), bytes);
                if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                    message.setTags(baseSendExtendDTO.getTag());
                }
                String keys = StrUtil.isEmpty(baseSendExtendDTO.getKeys()) ? UUID.randomUUID().toString() : baseSendExtendDTO.getKeys();
                message.setKeys(keys);

                message.setDeliverTimeMs(deliverTimeStamp); // 设置消息的送达时间，毫秒级 Unix 时间戳
                DefaultMQProducer defaultMQProducer = rocketMQTemplate.getProducer();

                sendResult = defaultMQProducer.send(message);
            }
            log.info("[生产者] {} - 发送结果：{}，消息ID：{}，消息Keys：{}", baseSendExtendDTO.getEventName(), sendResult.getSendStatus(), sendResult.getMsgId(), baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            log.error("[生产者] {} - 消息发送失败，消息体：{}", baseSendExtendDTO.getEventName(), JSON.toJSONString(messageSendEvent), ex);
            throw ex;
        }
        return sendResult;
    }
}
