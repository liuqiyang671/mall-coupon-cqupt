package com.mall.cqupt.framework.web;

import com.mall.cqupt.framework.errorcode.BaseErrorCode;
import com.mall.cqupt.framework.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsTest {

    @Test
    void successWithoutDataBuildsSuccessResult() {
        Result<Void> result = Results.success();

        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertTrue(result.isSuccess());
        assertFalse(result.isFail());
        assertNull(result.getData());
    }

    @Test
    void successWithDataCarriesPayload() {
        Result<String> result = Results.success("ok");

        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals("ok", result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    void failureBuildsServiceErrorResult() {
        Result<Void> result = Results.failure();

        assertFalse(result.isSuccess());
        assertTrue(result.isFail());
        assertEquals(BaseErrorCode.SERVICE_ERROR.code(), result.getCode());
        assertEquals(BaseErrorCode.SERVICE_ERROR.message(), result.getMessage());
    }
}
