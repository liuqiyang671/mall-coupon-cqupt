package com.mall.cqupt.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;

import com.mall.cqupt.merchant.admin.mq.base.BaseSendExtendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

import java.util.Optional;

/**
 * RocketMQ 抽象公共发送消息组件
 */
@RequiredArgsConstructor
@Slf4j(topic = "CommonSendProduceTemplate")// Lombok注解：自动生成日志实例，指定日志类别
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
        // 调用子类实现的方法，构建基础发送参数
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);

        // 声明发送结果变量
        SendResult sendResult;

        try {
            // 使用StringBuilder构建消息发送的目标地址
            // 目标地址格式为：topic:tag 或 topic（如果没有标签）
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());

            // 检查是否有标签，如果有则添加到目标地址
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }

            // 执行同步消息发送
            sendResult = rocketMQTemplate.syncSend(
                    destinationBuilder.toString(),                          // 消息目标地址
                    buildMessage(messageSendEvent, baseSendExtendDTO),     // 构建的消息对象
                    baseSendExtendDTO.getSentTimeout(),                    // 发送超时时间
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0)  // 延迟级别 如果为null则默认为0（不延迟）
            );

            // 记录消息发送成功的日志
            // 输出事件名称、发送状态、消息ID和消息键值等关键信息
            log.info("[生产者] {} - 发送结果：{}，消息ID：{}，消息Keys：{}",
                    baseSendExtendDTO.getEventName(),     // 事件名称
                    sendResult.getSendStatus(),           // 发送状态
                    sendResult.getMsgId(),               // 消息ID
                    baseSendExtendDTO.getKeys());        // 消息键值

        } catch (Throwable ex) {  // 捕获所有类型的异常，包括Error和Exception
            // 记录消息发送失败的日志，输出事件名称、消息体和异常堆栈
            log.error("[生产者] {} - 消息发送失败，消息体：{}",
                    baseSendExtendDTO.getEventName(),
                    JSON.toJSONString(messageSendEvent),  // 将消息体转为JSON字符串输出
                    ex);                                  // 异常堆栈信息

            // 重新抛出异常，让调用方能够感知到发送失败的情况
            throw ex;
        }

        // 返回消息发送结果
        return sendResult;
    }
}
