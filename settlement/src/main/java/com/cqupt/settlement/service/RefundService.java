package com.cqupt.settlement.service;

import java.math.BigDecimal;

public interface RefundService {
    boolean processRefund(Long orderId, BigDecimal refundAmount);
}
