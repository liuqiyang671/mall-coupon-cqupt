package com.mall.cqupt.engine.service;



import com.mall.cqupt.engine.dao.entity.PaymentDO;
import com.mall.cqupt.engine.gateway.PaymentGateway;

import java.math.BigDecimal;


public interface CouponPaymentService {
    PaymentDO createPaymentRecord(Long orderId, Long userId, BigDecimal paymentAmount, String paymentMethod);
    boolean processPayment(Long paymentId, PaymentGateway paymentGateway);
}
