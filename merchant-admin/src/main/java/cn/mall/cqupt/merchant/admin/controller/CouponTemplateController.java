package cn.mall.cqupt.merchant.admin.controller;

import cn.mall.cqupt.merchant.admin.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
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

}
