package com.mall.cqupt.merchant.admin.controller;

import com.mall.cqupt.framework.idempotent.NoRepeatSubmit;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.service.CouponTemplateService;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: liuqiyang
 * @CreateTime: 2026-01-19
 * @Description: 优惠券模板控制层
 */
@RestController
@RequiredArgsConstructor // 自动注入，为声明为final字段生成构造函数，不需要@Autowired
@Tag(name = "优惠券模板管理")
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @Operation(summary = "商家创建优惠券模板")
    @NoRepeatSubmit(message = "请勿短时间内重复提交优惠券模板")
    @PostMapping("/api/merchant-admin/coupon-template/save")
    public Result<Void> saveCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.createCouponTemplate(requestParam);
        return Results.success();
    }

}
