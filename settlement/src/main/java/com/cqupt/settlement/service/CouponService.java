package com.cqupt.settlement.service;



import com.cqupt.settlement.dao.entity.CouponTemplateDO;

import java.util.List;

public interface CouponService {

    /**
     * 查询用户可用的优惠券列表
     * @param userId 用户ID
     * @return 可用的优惠券列表
     */
    List<CouponTemplateDO> getAvailableCoupons(Long userId);

    /**
     * 查询用户不可用的优惠券列表
     * @param userId 用户ID
     * @return 不可用的优惠券列表
     */
    List<CouponTemplateDO> getUnavailableCoupons(Long userId);
}
