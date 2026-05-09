package com.cqupt.settlement.service.impl;

import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CouponApplyServiceImplTest {

    @Disabled("applySelectedCoupon is not implemented yet; enable this when coupon application logic is added.")
    @Test
    void applySelectedCouponShouldReturnDiscountedOrderResult() {
        ApplyCouponReqDTO request = new ApplyCouponReqDTO();
        request.setUserId(10L);
        request.setShopNumber(10001L);
        request.setOrderId(90001L);
        request.setOrderAmount(new BigDecimal("100.00"));

        assertNotNull(new CouponApplyServiceImpl().applySelectedCoupon(request, 20001L));
    }
}
