package com.mall.cqupt.lqy.distribution.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SendMessageMarkCovertEnumTest {

    @Test
    void fromTypeReturnsStrategyMark() {
        assertEquals("SITE", SendMessageMarkCovertEnum.fromType(0));
        assertEquals("APPLICATION", SendMessageMarkCovertEnum.fromType(1));
        assertEquals("EMAIL", SendMessageMarkCovertEnum.fromType(2));
        assertEquals("SMS", SendMessageMarkCovertEnum.fromType(3));
        assertEquals("WECHAT", SendMessageMarkCovertEnum.fromType(4));
    }

    @Test
    void fromTypeRejectsUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> SendMessageMarkCovertEnum.fromType(99));
    }
}
