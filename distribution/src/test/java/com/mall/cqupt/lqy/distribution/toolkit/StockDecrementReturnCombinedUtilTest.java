package com.mall.cqupt.lqy.distribution.toolkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockDecrementReturnCombinedUtilTest {

    @Test
    void combineAndExtractFieldsWhenStockDecrementSucceeds() {
        int combined = StockDecrementReturnCombinedUtil.combineFields(true, 345);

        assertTrue(StockDecrementReturnCombinedUtil.extractFirstField(combined));
        assertEquals(345L, StockDecrementReturnCombinedUtil.extractSecondField(combined));
    }

    @Test
    void combineAndExtractFieldsWhenStockDecrementFails() {
        int combined = StockDecrementReturnCombinedUtil.combineFields(false, 0);

        assertFalse(StockDecrementReturnCombinedUtil.extractFirstField(combined));
        assertEquals(0L, StockDecrementReturnCombinedUtil.extractSecondField(combined));
    }
}
