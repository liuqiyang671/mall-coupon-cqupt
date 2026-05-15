package com.mall.cqupt.search.mq.base;

import com.mall.cqupt.search.common.enums.CouponTemplateStatusEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageWrapperTest {

    @Test
    void createsWrapperWithDefaultTraceFields() {
        MessageWrapper<String> wrapper = new MessageWrapper<>("coupon-template-sync", "payload");

        assertThat(wrapper.getKeys()).isEqualTo("coupon-template-sync");
        assertThat(wrapper.getMessage()).isEqualTo("payload");
        assertThat(wrapper.getUuid()).isNotBlank();
        assertThat(wrapper.getTimestamp()).isPositive();
    }

    @Test
    void buildsSendExtensionMetadata() {
        BaseSendExtendDTO metadata = BaseSendExtendDTO.builder()
                .eventName("coupon-template-created")
                .topic("coupon-template")
                .tag("create")
                .keys("template-1001")
                .sentTimeout(3000L)
                .delayLevel(2)
                .build();

        assertThat(metadata.getEventName()).isEqualTo("coupon-template-created");
        assertThat(metadata.getTopic()).isEqualTo("coupon-template");
        assertThat(metadata.getTag()).isEqualTo("create");
        assertThat(metadata.getKeys()).isEqualTo("template-1001");
        assertThat(metadata.getSentTimeout()).isEqualTo(3000L);
        assertThat(metadata.getDelayLevel()).isEqualTo(2);
    }

    @Test
    void exposesCouponTemplateStatusCodes() {
        assertThat(CouponTemplateStatusEnum.EFFECTIVE.getCode()).isZero();
        assertThat(CouponTemplateStatusEnum.EXPIRED.getCode()).isOne();
        assertThat(CouponTemplateStatusEnum.NORMAL.getCode()).isZero();
        assertThat(CouponTemplateStatusEnum.DELETED.getCode()).isOne();
    }
}
