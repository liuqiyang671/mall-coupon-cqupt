package com.cqupt.settlement.service;

import com.cqupt.settlement.dao.entity.PaymentDO;
import com.cqupt.settlement.gateway.PaymentGateway;

import java.math.BigDecimal;


public interface PaymentService {
    PaymentDO createPaymentRecord(Long orderId, Long userId, BigDecimal paymentAmount, String paymentMethod);
    boolean processPayment(Long paymentId, PaymentGateway paymentGateway);
}
