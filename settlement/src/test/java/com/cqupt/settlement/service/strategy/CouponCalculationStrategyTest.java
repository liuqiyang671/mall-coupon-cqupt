package com.cqupt.settlement.service.strategy;

import com.cqupt.settlement.dao.entity.DiscountCouponDO;
import com.cqupt.settlement.dao.entity.FixedDiscountCouponDO;
import com.cqupt.settlement.dao.entity.ThresholdCouponDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CouponCalculationStrategyTest {

    @Test
    void fixedDiscountReturnsConfiguredAmount() {
        FixedDiscountCouponDO coupon = new FixedDiscountCouponDO();
        coupon.setDiscountAmount(30);

        BigDecimal discount = new FixedDiscountCalculationStrategy()
                .calculateDiscount(coupon, new BigDecimal("199.00"));

        assertEquals(0, new BigDecimal("30").compareTo(discount));
    }

    @Test
    void thresholdDiscountReturnsDiscountWhenOrderMeetsThreshold() {
        ThresholdCouponDO coupon = new ThresholdCouponDO();
        coupon.setThresholdAmount(100);
        coupon.setDiscountAmount(20);

        BigDecimal discount = new ThresholdCalculationStrategy()
                .calculateDiscount(coupon, new BigDecimal("100.00"));

        assertEquals(0, new BigDecimal("20").compareTo(discount));
    }

    @Test
    void thresholdDiscountReturnsZeroWhenOrderBelowThreshold() {
        ThresholdCouponDO coupon = new ThresholdCouponDO();
        coupon.setThresholdAmount(100);
        coupon.setDiscountAmount(20);

        BigDecimal discount = new ThresholdCalculationStrategy()
                .calculateDiscount(coupon, new BigDecimal("99.99"));

        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }

    @Test
    void discountCouponMultipliesOrderAmountByDiscountRate() {
        DiscountCouponDO coupon = new DiscountCouponDO();
        coupon.setDiscountRate(0.8D);

        BigDecimal discount = new DiscountCalculationStrategy()
                .calculateDiscount(coupon, new BigDecimal("150.00"));

        assertEquals(0, new BigDecimal("120.00").compareTo(discount));
    }
}
