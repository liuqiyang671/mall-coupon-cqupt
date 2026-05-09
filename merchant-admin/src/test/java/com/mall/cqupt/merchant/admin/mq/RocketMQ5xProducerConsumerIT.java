package com.mall.cqupt.merchant.admin.mq;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RocketMQ producer-consumer integration test with a bounded consume timeout.
 */
@Slf4j
@Tag("integration")
@SpringBootTest
public final class RocketMQ5xProducerConsumerIT {

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
        SendResult sendResult = rocketMQTemplate.syncSend("MerchantAdminTestTopic", "TestMessage");
        log.info("RocketMQ message send result: {}", sendResult);

        Assertions.assertTrue(
                messageConsumedLatch.await(10, TimeUnit.SECONDS),
                "RocketMQ message was not consumed in time"
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
            topic = "MerchantAdminTestTopic",
            consumerGroup = "MerchantAdminTestTopic_CG"
    )
    static class RocketMQ5xConsumerIT implements RocketMQListener<String> {

        @Override
        public void onMessage(String message) {
            log.info("RocketMQ message consumed: {}", message);
            CONSUMED_MESSAGE.set(message);
            messageConsumedLatch.countDown();
        }
    }
}
