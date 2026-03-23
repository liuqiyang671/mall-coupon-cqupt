package com.cqupt.settlement.common.util;


import com.cqupt.settlement.common.enums.DiscountTypeEnum;
import com.cqupt.settlement.dao.entity.CouponTemplateDO;
import com.cqupt.settlement.dao.entity.DiscountCouponDO;
import com.cqupt.settlement.dao.entity.FixedDiscountCouponDO;
import com.cqupt.settlement.dao.entity.ThresholdCouponDO;
import com.cqupt.settlement.service.strategy.CouponCalculationStrategy;
import com.cqupt.settlement.service.strategy.DiscountCalculationStrategy;
import com.cqupt.settlement.service.strategy.FixedDiscountCalculationStrategy;
import com.cqupt.settlement.service.strategy.ThresholdCalculationStrategy;

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
    /**
     * 获取优惠券计算策略。
     *
     * @param coupon 基础优惠券模板对象
     * @return 对应的优惠券计算策略
     */
    public static CouponCalculationStrategy getCouponCalculationStrategy(CouponTemplateDO coupon) {
        switch (DiscountTypeEnum.values()[coupon.getType()]) {
            case FIXED_DISCOUNT:
                return new FixedDiscountCalculationStrategy();
            case THRESHOLD_DISCOUNT:
                return new ThresholdCalculationStrategy();
            case DISCOUNT_COUPON:
                return new DiscountCalculationStrategy();
            default:
                throw new IllegalArgumentException("Invalid coupon type");
        }
    }
}