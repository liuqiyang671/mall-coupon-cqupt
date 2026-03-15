package com.cqupt.settlement.service.strategy;



import com.cqupt.settlement.dao.entity.CouponTemplateDO;

import java.util.List;

public interface CouponQueryStrategy {
    List<CouponTemplateDO> queryCoupons(Long userId, boolean available);
}
