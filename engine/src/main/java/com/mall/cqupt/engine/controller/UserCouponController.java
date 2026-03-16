package com.mall.cqupt.engine.controller;


import com.mall.cqupt.engine.dto.req.CouponCreatePaymentReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.mall.cqupt.engine.service.CouponPayService;
import com.mall.cqupt.engine.service.UserCouponService;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户优惠券控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "用户优惠券管理")
public class UserCouponController {

    private final UserCouponService userCouponService;
    private final CouponPayService couponPayService;

    @Operation(summary = "兑换优惠券模板", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/api/engine/user-coupon/redeem")
    public Result<Void> redeemUserCoupon(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        userCouponService.redeemUserCoupon(requestParam);
        return Results.success();
    }

    @Operation(summary = "创建用户优惠券结算单", description = "用户下单时锁定使用的优惠券，一般由订单系统发起调用")
    @PostMapping("/api/engine/user-coupon/create-payment-record")
    public Result<Void> createPaymentRecord(@RequestBody CouponCreatePaymentReqDTO requestParam) {
        couponPayService.createPaymentRecord(requestParam);
        return Results.success();
    }
}
