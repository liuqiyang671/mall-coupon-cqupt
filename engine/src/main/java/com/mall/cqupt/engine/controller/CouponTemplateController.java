package com.mall.cqupt.engine.controller;


import com.mall.cqupt.engine.dto.req.CouponTemplateQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "查询优惠券模板")
    @GetMapping("/api/engine/coupon-template/query")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.findCouponTemplate(requestParam));
    }

    @Operation(summary = "兑换优惠券模板", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/api/engine/coupon-template/redeem")
    public Result<Void> redeemCouponTemplate(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        couponTemplateService.redeemCouponTemplate(requestParam);
        return Results.success();
    }
}
