package com.cqupt.settlement.service.impl;


import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import com.cqupt.settlement.dto.resp.ApplyCouponRespDTO;
import com.cqupt.settlement.service.CouponApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 优惠券应用服务实现类
 */
@Service
@RequiredArgsConstructor
public class CouponApplyServiceImpl implements CouponApplyService {

    @Override
    public ApplyCouponRespDTO applySelectedCoupon(ApplyCouponReqDTO applyCouponReqDTO, Long selectedCouponId) {
        return null;
    }
}
