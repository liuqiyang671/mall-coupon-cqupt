package com.mall.cqupt.engine.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindPageQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindPageQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import com.mall.cqupt.framework.idempotent.NoRepeatSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券预约提醒管理")
public class CouponTemplateRemindController {

    private final CouponTemplateRemindService couponTemplateRemindService;

    @Operation(summary = "发出优惠券预约提醒请求")
    @NoRepeatSubmit(message = "请勿短时间内重复提交预约提醒请求")
    @PostMapping("/api/engine/coupon-template-remind/create")
    public Result<Boolean> createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam) {
        return Results.success(couponTemplateRemindService.createCouponRemind(requestParam));
    }

    @Operation(summary = "查询优惠券预约提醒")
    @GetMapping("/api/engine/coupon-template-remind/page")
    public Result<IPage<CouponTemplateRemindPageQueryRespDTO>> pageQueryCouponRemind(CouponTemplateRemindPageQueryReqDTO requestParam) {
        return Results.success(couponTemplateRemindService.pageQueryCouponRemind(requestParam));
    }

    @Operation(summary = "取消优惠券预约提醒")
    @PostMapping("/api/engine/coupon-template-remind/cancel")
    public Result<Boolean> cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam) {
        return Results.success(couponTemplateRemindService.cancelCouponRemind(requestParam));
    }
}
