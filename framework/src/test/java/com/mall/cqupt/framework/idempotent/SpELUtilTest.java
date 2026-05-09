package com.mall.cqupt.framework.idempotent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpELUtilTest {

    @Test
    void parseKeyReturnsLiteralWhenExpressionHasNoSpelFlag() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("submit");

        Object key = SpELUtil.parseKey("coupon:create", method, new Object[0]);

        assertEquals("coupon:create", key);
    }

    @Test
    void parseKeyEvaluatesStaticSpelExpression() throws NoSuchMethodException {
        Method method = SampleService.class.getDeclaredMethod("submit");

        Object key = SpELUtil.parseKey("T(java.lang.String).valueOf(1001)", method, new Object[0]);

        assertEquals("1001", key);
    }

    private static class SampleService {
        @SuppressWarnings("unused")
        void submit() {
        }
    }
}
