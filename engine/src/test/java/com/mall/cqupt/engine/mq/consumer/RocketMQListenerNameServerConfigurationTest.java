package com.mall.cqupt.engine.mq.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMQListenerNameServerConfigurationTest {

    @Test
    void engineConsumersShouldUseConfigurableNameServer() {
        List<Class<?>> consumers = List.of(
                UserCouponRedeemConsumer.class,
                UserCouponDelayCloseConsumer.class,
                CouponRemindConsumer.class,
                CanalBinlogSyncUserCouponConsumer.class
        );

        assertThat(consumers)
                .allSatisfy(consumer -> assertThat(consumer.getAnnotation(RocketMQMessageListener.class).nameServer())
                        .as("%s nameServer", consumer.getSimpleName())
                        .isEqualTo("${rocketmq.name-server:}"));
    }
}
