package com.mall.cqupt.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.framework.idempotent.NoRepeatSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTaskPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTaskPageQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.CouponTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券推送任务控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "优惠券推送任务管理")
public class CouponTaskController {

    private final CouponTaskService couponTaskService;

    @Operation(summary = "商家创建优惠券推送任务")
    @NoRepeatSubmit(message = "请勿短时间内重复提交优惠券推送任务")
    @PostMapping("/api/merchant-admin/coupon-task/create")
    public Result<Void> createCouponTask(@RequestBody CouponTaskCreateReqDTO requestParam) {
        couponTaskService.createCouponTask(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询优惠券推送任务")
    @GetMapping("/api/merchant-admin/coupon-task/page")
    public Result<IPage<CouponTaskPageQueryRespDTO>> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam) {
        return Results.success(couponTaskService.pageQueryCouponTask(requestParam));
    }
}
