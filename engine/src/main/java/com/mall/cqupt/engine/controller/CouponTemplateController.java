package com.mall.cqupt.engine.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.engine.dto.req.CouponTemplatePageQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateQueryReqDTO;
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

    @Operation(summary = "分页查询领券中心可领取优惠券")
    @GetMapping("/api/engine/coupon-template/page")
    public Result<IPage<CouponTemplateQueryRespDTO>> pageAvailableCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.pageAvailableCouponTemplate(requestParam));
    }
}
