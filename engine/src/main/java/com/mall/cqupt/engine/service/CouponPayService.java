package com.mall.cqupt.engine.service;


import com.mall.cqupt.engine.dto.req.CouponCreatePaymentReqDTO;
import com.mall.cqupt.engine.dto.req.CouponProcessPaymentReqDTO;
import com.mall.cqupt.engine.dto.req.CouponProcessRefundReqDTO;

/**
 * 优惠券支付服务相关接口层
 */
public interface CouponPayService {

    /**
     * 创建优惠券结算单记录
     *
     * @param requestParam 创建优惠券结算单请求参数
     */
    void createPaymentRecord(CouponCreatePaymentReqDTO requestParam);

    /**
     * 处理订单支付操作，修改结算单为已支付
     *
     * @param requestParam 处理优惠券结算单请求参数
     */
    void processPayment(CouponProcessPaymentReqDTO requestParam);

    /**
     * 处理订单退款操作，修改结算单为已退款并回滚优惠券
     *
     * @param requestParam 处理优惠券结算单退款请求参数
     */
    void processRefund(CouponProcessRefundReqDTO requestParam);
}
