package com.cqupt.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;
import com.cqupt.settlement.service.CouponQueryService;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询用户优惠券控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "查询用户优惠券")
public class CouponQueryController {
    private final CouponQueryService couponQueryService;

    @Operation(summary = "分页查询用户可用的优惠券列表")
    @GetMapping("/api/settlement/coupon-query/page/available")
    public Result<IPage<QueryCouponsRespDTO>> pageQueryAvailableCoupons(QueryCouponsReqDTO requestParam) {
        return Results.success(couponQueryService.pageQueryAvailableCoupons(requestParam));
    }

    @Operation(summary = "分页查询用户不可用的优惠券列表")
    @GetMapping("/api/settlement/coupon-query/page/unavailable")
    public Result<IPage<QueryCouponsRespDTO>> pageQueryUnavailableCoupons(QueryCouponsReqDTO requestParam) {
        return Results.success(couponQueryService.pageQueryUnavailableCoupons(requestParam));
    }
}