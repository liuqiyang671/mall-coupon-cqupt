package com.cqupt.settlement.service;


import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import com.cqupt.settlement.dto.resp.ApplyCouponRespDTO;
import org.springframework.stereotype.Service;

/**
 * CouponApplyService 是一个用于处理用户选择优惠券并应用折扣的服务接口
 * 该接口定义了一个方法，用于接收用户的订单信息和用户选择的优惠券
 * 计算并返回应用优惠券后的最终金额
 */
@Service
public interface CouponApplyService {

    /**
     * 计算订单折扣后金额
     *
     * @param requestParam     请求参数
     * @param selectedCouponId 用户优惠券 ID
     * @return 订单折扣后金额
     */
    ApplyCouponRespDTO applySelectedCoupon(ApplyCouponReqDTO requestParam, Long selectedCouponId);
}
