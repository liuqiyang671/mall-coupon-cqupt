package com.mall.cqupt.engine.toolkit;

import com.mall.cqupt.engine.common.enums.CouponRemindTypeEnum;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.framework.exception.ClientException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponTemplateRemindUtilTest {

    @Test
    void calculateBitMapPacksTimeAndTypeIntoExpectedBit() {
        assertEquals(1L, CouponTemplateRemindUtil.calculateBitMap(5, CouponRemindTypeEnum.EMAIL.getType()));
        assertEquals(1L << 13, CouponTemplateRemindUtil.calculateBitMap(10, CouponRemindTypeEnum.MESSAGE.getType()));
    }

    @Test
    void calculateBitMapRejectsTimeBeyondOneHour() {
        assertThrows(ClientException.class, () -> CouponTemplateRemindUtil.calculateBitMap(65, 0));
    }

    @Test
    void fillRemindInformationRestoresTimesAndTypesFromBitmap() {
        Date validStartTime = Date.from(Instant.parse("2026-05-09T10:00:00Z"));
        Long information = CouponTemplateRemindUtil.calculateBitMap(5, CouponRemindTypeEnum.EMAIL.getType())
                | CouponTemplateRemindUtil.calculateBitMap(10, CouponRemindTypeEnum.MESSAGE.getType());
        CouponTemplateRemindQueryRespDTO resp = new CouponTemplateRemindQueryRespDTO();
        resp.setValidStartTime(validStartTime);

        CouponTemplateRemindUtil.fillRemindInformation(resp, information);

        assertEquals(2, resp.getRemindTime().size());
        assertEquals(new Date(validStartTime.getTime() - 5 * 60 * 1000L), resp.getRemindTime().get(0));
        assertEquals(new Date(validStartTime.getTime() - 10 * 60 * 1000L), resp.getRemindTime().get(1));
        assertEquals(CouponRemindTypeEnum.EMAIL.getDescribe(), resp.getRemindType().get(0));
        assertEquals(CouponRemindTypeEnum.MESSAGE.getDescribe(), resp.getRemindType().get(1));
    }
}
