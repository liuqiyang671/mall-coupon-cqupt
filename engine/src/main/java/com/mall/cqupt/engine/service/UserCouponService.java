package com.mall.cqupt.engine.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.engine.dto.req.UserCouponPageQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.mall.cqupt.engine.dto.resp.UserCouponPageQueryRespDTO;

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

    /**
     * 用户兑换优惠券消息队列版本
     * 支持更高的并发，完全依赖缓存做前置校验，如果缓存认为没问题，直接返回用户请求成功，在消息队列中做扣减等一系列流程
     *
     * @param requestParam 请求参数
     */
    void redeemUserCouponByMQ(CouponTemplateRedeemReqDTO requestParam);

    /**
     * 分页查询当前用户券包
     *
     * @param requestParam 请求参数
     * @return 用户券包分页数据
     */
    IPage<UserCouponPageQueryRespDTO> pageUserCoupon(UserCouponPageQueryReqDTO requestParam);
}
