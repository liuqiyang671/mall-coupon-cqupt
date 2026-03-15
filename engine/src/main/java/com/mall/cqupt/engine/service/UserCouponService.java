package com.mall.cqupt.engine.service;


import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;

/**
 * 用户优惠券业务逻辑层
 */
public interface UserCouponService {

    /**
     * 用户兑换优惠券
     *
     * @param requestParam 请求参数
     */
    void redeemUserCoupon(CouponTemplateRedeemReqDTO requestParam);
}
