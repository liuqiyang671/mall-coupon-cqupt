package com.mall.cqupt.merchant.admin.mq;

import cn.hutool.core.date.DateUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RocketMQ delayed-message integration test with a bounded consume timeout.
 */
@Slf4j
@Tag("integration")
@SpringBootTest
public final class RocketMQ5xDelayProducerConsumerIT {

    private static final AtomicReference<String> CONSUMED_MESSAGE = new AtomicReference<>();
    private static volatile CountDownLatch messageConsumedLatch;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @BeforeEach
    void setUp() {
        CONSUMED_MESSAGE.set(null);
        messageConsumedLatch = new CountDownLatch(1);
    }

    @SneakyThrows
    @Test
    public void producerSendTest() {
        Message message = new Message("MerchantAdminTestDelayTopic", "TestMessage".getBytes(StandardCharsets.UTF_8));
        Long deliverTimeStamp = System.currentTimeMillis() + 4L * 1000;
        message.setDeliverTimeMs(deliverTimeStamp);
        DefaultMQProducer defaultMQProducer = rocketMQTemplate.getProducer();
        SendResult sendResult = defaultMQProducer.send(message);
        log.info("Delay message send result: {}", sendResult);
        log.info("Delay message sent at: {}", DateUtil.formatTime(new Date()));

        Assertions.assertTrue(
                messageConsumedLatch.await(15, TimeUnit.SECONDS),
                "RocketMQ delay message was not consumed in time"
        );
        Assertions.assertEquals("TestMessage", CONSUMED_MESSAGE.get());
    }

    @TestConfiguration
    static class RocketMQ5xConfiguration {

        @Bean
        public RocketMQ5xConsumerIT rocketMQ5xConsumerIT() {
            return new RocketMQ5xConsumerIT();
        }
    }

    @RocketMQMessageListener(
            topic = "MerchantAdminTestDelayTopic",
            consumerGroup = "MerchantAdminTestDelayTopic_CG"
    )
    static class RocketMQ5xConsumerIT implements RocketMQListener<String> {

        @Override
        public void onMessage(String message) {
            log.info("Delay message consumed: {}, received at: {}", message, DateUtil.formatTime(new Date()));
            CONSUMED_MESSAGE.set(message);
            messageConsumedLatch.countDown();
        }
    }
}
