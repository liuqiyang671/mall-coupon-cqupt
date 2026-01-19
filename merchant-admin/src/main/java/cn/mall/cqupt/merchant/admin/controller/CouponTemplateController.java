package cn.mall.cqupt.merchant.admin.controller;

import cn.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import cn.mall.cqupt.merchant.admin.service.CouponTemplateService;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.result.Results;
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
public class CouponTemplateController {

    private final CouponTemplateService couponTemplateService;

    @PostMapping("/api/merchant-admin/coupon-template/save")
    public Result<Void> saveCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.saveCouponTemplate(requestParam);
        return Results.success();
    }

}
