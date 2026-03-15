package com.mall.cqupt.engine.service;

import java.math.BigDecimal;

public interface CouponRefundService {
    boolean processRefund(Long orderId, BigDecimal refundAmount);
}
