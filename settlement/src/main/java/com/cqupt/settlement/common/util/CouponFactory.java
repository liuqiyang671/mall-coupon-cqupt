package com.cqupt.settlement.common.util;


import com.cqupt.settlement.common.enums.DiscountTypeEnum;
import com.cqupt.settlement.dao.entity.CouponTemplateDO;
import com.cqupt.settlement.dao.entity.DiscountCouponDO;
import com.cqupt.settlement.dao.entity.FixedDiscountCouponDO;
import com.cqupt.settlement.dao.entity.ThresholdCouponDO;

import java.util.Map;

/**
 * 优惠券工厂类
 */
public class CouponFactory {
    public static CouponTemplateDO createCoupon(CouponTemplateDO coupon, Map<String, Object> additionalParams) {
        if (coupon.getType() == null || coupon.getType() >= DiscountTypeEnum.values().length || coupon.getType() < 0) {
            throw new IllegalArgumentException("Invalid coupon type");
        }
        switch (DiscountTypeEnum.values()[coupon.getType()]) {
            case FIXED_DISCOUNT:
                Integer fixedDiscountAmount = (Integer) additionalParams.get("discountAmount");
                return new FixedDiscountCouponDO(coupon, fixedDiscountAmount);
            case THRESHOLD_DISCOUNT:
                Integer thresholdAmount = (Integer) additionalParams.get("thresholdAmount");
                Integer thresholdDiscountAmount = (Integer) additionalParams.get("discountAmount");
                return new ThresholdCouponDO(coupon, thresholdAmount, thresholdDiscountAmount);
            case DISCOUNT_COUPON:
                Double discountRate = (Double) additionalParams.get("discountRate");
                return new DiscountCouponDO(coupon, discountRate);
            default:
                throw new IllegalArgumentException("Invalid coupon type");
        }
    }
}