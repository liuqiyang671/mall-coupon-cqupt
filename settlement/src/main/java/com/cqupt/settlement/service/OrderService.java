package com.cqupt.settlement.service;


import com.cqupt.settlement.dao.entity.OrderDO;

import java.math.BigDecimal;
import java.util.List;


public interface OrderService {
    OrderDO createUserOrder(Long userId, String shopNumber, BigDecimal totalAmount, BigDecimal payableAmount, Long couponId, BigDecimal couponAmount);
    List<OrderDO> getUserOrder(Long userId);
    OrderDO getOrderDetail(Long orderId);
}
