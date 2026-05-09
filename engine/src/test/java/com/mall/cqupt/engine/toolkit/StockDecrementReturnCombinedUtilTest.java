package com.mall.cqupt.engine.toolkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockDecrementReturnCombinedUtilTest {

    @Test
    void extractsPackedFieldsFromLuaResult() {
        long combined = (2L << 14) | 1234L;

        assertEquals(2L, StockDecrementReturnCombinedUtil.extractFirstField(combined));
        assertEquals(1234L, StockDecrementReturnCombinedUtil.extractSecondField(combined));
    }

    @Test
    void secondFieldUsesLowerFourteenBits() {
        long combined = (1L << 14) | 9999L;

        assertEquals(1L, StockDecrementReturnCombinedUtil.extractFirstField(combined));
        assertEquals(9999L, StockDecrementReturnCombinedUtil.extractSecondField(combined));
    }
}
