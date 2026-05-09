package com.mall.cqupt.merchant.admin;

import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson2.JSONObject;
import com.mall.cqupt.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.service.CouponTemplateService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Date;

/**
 * MySQL-backed coupon template integration test, isolated from default unit tests.
 */
@Tag("integration")
@SpringBootTest
class MerchantAdminApplicationIT {

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Test
    public void testInsertCouponTemplate() {
        JSONObject receiveRule = new JSONObject();
        receiveRule.put("limitPerPerson", 1);
        receiveRule.put("usageInstructions", "3");
        JSONObject consumeRule = new JSONObject();
        consumeRule.put("termsOfUse", new BigDecimal("10"));
        consumeRule.put("maximumDiscountAmount", new BigDecimal("3"));
        consumeRule.put("explanationOfUnmetConditions", "3");
        consumeRule.put("validityPeriod", new Date());
        CouponTemplateDO couponTemplateDO = CouponTemplateDO.builder()
                .name("goods-direct-discount-coupon")
                .source(0)
                .target(1)
                .type(0)
                .validStartTime(new Date())
                .validEndTime(new Date())
                .stock(10)
                .receiveRule(receiveRule.toString())
                .consumeRule(consumeRule.toString())
                .status(CouponTemplateStatusEnum.ACTIVE.getStatus())
                .build();
        boolean saved = couponTemplateService.save(couponTemplateDO);
        Assert.isTrue(saved);
    }
}
