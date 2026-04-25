package com.cqupt.settlement.controller;


import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import com.cqupt.settlement.dto.resp.ApplyCouponRespDTO;
import com.cqupt.settlement.service.CouponApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 使用用户优惠券控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券结算管理")
public class CouponApplyController {

    private final CouponApplyService couponApplyService;

    @Operation(summary = "应用优惠券折扣订单金额")
    @PostMapping("/api/settlement/apply-coupon/{couponId}")
    public ResponseEntity<ApplyCouponRespDTO> applySelectedCoupon(@RequestBody ApplyCouponReqDTO applyCouponReqDTO,
                                                                  @PathVariable Long couponId) {
        ApplyCouponRespDTO response = couponApplyService.applySelectedCoupon(applyCouponReqDTO, couponId);
        return ResponseEntity.ok(response);
    }
}
