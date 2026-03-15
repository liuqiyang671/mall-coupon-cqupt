package com.cqupt.settlement.service;

import java.math.BigDecimal;
import java.util.List;


public interface CouponSettlementService {

    /**
     * 计算订单总金额
     * @param orderId 订单ID
     * @return 订单总金额
     */
    BigDecimal calculateTotalAmount(Long orderId);

    /**
     * 应用优惠券，计算最终价格
     * @param orderId 订单ID
     * @param couponIds 优惠券ID列表
     * @return 最终应付金额
     */
    BigDecimal applyCoupons(Long orderId, List<Long> couponIds);
}
