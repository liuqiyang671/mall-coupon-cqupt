package com.mall.cqupt.merchant.admin.service.handler.filter;

import cn.hutool.core.util.ObjectUtil;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.merchant.admin.common.enums.DiscountTargetEnum;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.mall.cqupt.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;


/**
 * 验证优惠券创建接口参数是否正确责任链｜验证参数数据是否正确
 */
@Component
public class CouponTemplateCreateParamVerifyChainFilter implements MerchantAdminAbstractChainHandler<CouponTemplateSaveReqDTO> {

    @Override
    public void handler(CouponTemplateSaveReqDTO requestParam) {
        if (ObjectUtil.equal(requestParam.getTarget(), DiscountTargetEnum.PRODUCT_SPECIFIC)) {
            Integer type = requestParam.getType();
            String consumeRuleStr = requestParam.getConsumeRule();

            // 1. 基础校验：确保 consume_rule 不是空，且是合法的 JSON
            if (!JSON.isValid(consumeRuleStr)) {
                throw new ClientException("消耗规则格式错误，存在恶意篡改风险");
            }
            JSONObject consumeRule = JSON.parseObject(consumeRuleStr);
            switch (type) {
                case 0:
                    // 【立减券 (无门槛券)】逻辑校验
                    verifyNoThresholdCoupon(consumeRule);
                    break;
                case 1:
                    // 【满减券】逻辑校验
                    verifyThresholdCoupon(consumeRule);
                    break;
                case 2:
                    // 【折扣券】逻辑校验
                    verifyDiscountCoupon(consumeRule);
                    break;
                default:
                    throw new ClientException("未知的优惠券类型");
            }
        }
    }

    /**
     * 校验立减券规则
     */
    private void verifyNoThresholdCoupon(JSONObject consumeRule) {
        BigDecimal discountAmount = consumeRule.getBigDecimal("discountAmount");
        BigDecimal thresholdAmount = consumeRule.getBigDecimal("thresholdAmount");

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ClientException("立减券必须配置大于 0 的立减金额");
        }

        // 校验门槛金额：必须不存在，或者是 0
        if (thresholdAmount != null && thresholdAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new ClientException("业务逻辑冲突：立减券属于无门槛券，不能配置满减门槛金额");
        }
    }

    /**
     * 校验满减券规则
     */
    private void verifyThresholdCoupon(JSONObject consumeRule) {
        BigDecimal discountAmount = consumeRule.getBigDecimal("discountAmount");
        BigDecimal thresholdAmount = consumeRule.getBigDecimal("thresholdAmount");

        if (discountAmount == null || thresholdAmount == null) {
            throw new ClientException("满减券必须同时配置【满减门槛】和【扣减金额】");
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0 || thresholdAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ClientException("满减金额及门槛必须大于 0");
        }

        // 满减门槛必须严格大于扣减金额
        if (thresholdAmount.compareTo(discountAmount) <= 0) {
            throw new ClientException("高危资损风险拦截：满减门槛必须大于扣减金额！(例如不支持满20减100)");
        }
    }

    /**
     * 校验折扣券规则
     */
    private void verifyDiscountCoupon(JSONObject consumeRule) {
        BigDecimal discountRate = consumeRule.getBigDecimal("discountRate");
        BigDecimal maxDiscountAmount = consumeRule.getBigDecimal("maxDiscountAmount");

        if (discountRate == null) {
            throw new ClientException("折扣券必须配置折扣率");
        }

        // 校验折扣率范围：假设限制在 0.1 折 到 9.9 折之间
        BigDecimal minRate = new BigDecimal("0.1");
        BigDecimal maxRate = new BigDecimal("9.9");
        if (discountRate.compareTo(minRate) < 0 || discountRate.compareTo(maxRate) > 0) {
            throw new ClientException("折扣率设置异常，必须在 0.1 ~ 9.9 折之间");
        }

        // 设置最大抵扣上限
        if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ClientException("高危破产风险拦截：折扣券强制要求配置【最大抵扣金额】上限！");
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
