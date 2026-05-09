package com.cqupt.settlement.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cqupt.settlement.common.util.CouponFactory;
import com.cqupt.settlement.dao.entity.CouponTemplateDO;
import com.cqupt.settlement.dao.entity.UserCouponDO;
import com.cqupt.settlement.dao.mapper.CouponTemplateMapper;
import com.cqupt.settlement.dao.mapper.UserCouponMapper;
import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import com.cqupt.settlement.dto.req.QueryCouponGoodsReqDTO;
import com.cqupt.settlement.dto.resp.ApplyCouponRespDTO;
import com.cqupt.settlement.service.CouponApplyService;
import com.cqupt.settlement.service.CouponCalculationService;
import com.mall.cqupt.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Applies a selected coupon after validating ownership, status, validity and goods scope.
 */
@Service
@RequiredArgsConstructor
public class CouponApplyServiceImpl implements CouponApplyService {

    private static final int UNUSED_USER_COUPON_STATUS = 0;
    private static final int ACTIVE_TEMPLATE_STATUS = 0;
    private static final int GOODS_COUPON_TARGET = 0;
    private static final int DELETED_FLAG = 1;

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponCalculationService couponCalculationService;

    @Override
    public ApplyCouponRespDTO applySelectedCoupon(ApplyCouponReqDTO requestParam, Long selectedCouponId) {
        validateRequest(requestParam, selectedCouponId);

        UserCouponDO userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCouponDO>()
                .eq(UserCouponDO::getUserId, requestParam.getUserId())
                .eq(UserCouponDO::getCouponTemplateId, selectedCouponId)
                .eq(UserCouponDO::getStatus, UNUSED_USER_COUPON_STATUS)
                .eq(UserCouponDO::getDelFlag, 0));
        validateUserCoupon(userCoupon, requestParam, selectedCouponId);

        CouponTemplateDO couponTemplate = couponTemplateMapper.selectById(selectedCouponId);
        validateCouponTemplate(couponTemplate, requestParam);

        BigDecimal calculationAmount = resolveCalculationAmount(requestParam, couponTemplate);
        CouponTemplateDO calculationCoupon = buildCalculationCoupon(couponTemplate);
        BigDecimal couponAmount = couponCalculationService.calculateDiscount(calculationCoupon, calculationAmount);
        couponAmount = capByMaximumDiscount(couponTemplate, couponAmount);
        validateCouponAmount(couponAmount);

        BigDecimal finalAmount = requestParam.getOrderAmount().subtract(couponAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        return ApplyCouponRespDTO.builder()
                .orderId(requestParam.getOrderId())
                .originalAmount(requestParam.getOrderAmount())
                .finalAmount(finalAmount)
                .appliedCouponId(selectedCouponId)
                .build();
    }

    private void validateRequest(ApplyCouponReqDTO requestParam, Long selectedCouponId) {
        if (requestParam == null || selectedCouponId == null) {
            throw new ClientException("优惠券结算参数不能为空");
        }
        if (requestParam.getUserId() == null || requestParam.getShopNumber() == null
                || requestParam.getOrderId() == null || requestParam.getOrderAmount() == null) {
            throw new ClientException("优惠券结算参数不完整");
        }
        if (requestParam.getOrderAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ClientException("订单金额不能为负数");
        }
    }

    private void validateUserCoupon(UserCouponDO userCoupon, ApplyCouponReqDTO requestParam, Long selectedCouponId) {
        if (userCoupon == null
                || !Objects.equals(userCoupon.getUserId(), requestParam.getUserId())
                || !Objects.equals(userCoupon.getCouponTemplateId(), selectedCouponId)) {
            throw new ClientException("优惠券不存在或不属于当前用户");
        }
        if (!Objects.equals(userCoupon.getStatus(), UNUSED_USER_COUPON_STATUS) || Objects.equals(userCoupon.getDelFlag(), DELETED_FLAG)) {
            throw new ClientException("优惠券当前不可用");
        }
        validateTimeRange(userCoupon.getValidStartTime(), userCoupon.getValidEndTime(), "优惠券已过期或未到可用时间");
    }

    private void validateCouponTemplate(CouponTemplateDO couponTemplate, ApplyCouponReqDTO requestParam) {
        if (couponTemplate == null) {
            throw new ClientException("优惠券模板不存在");
        }
        if (!Objects.equals(couponTemplate.getShopNumber(), requestParam.getShopNumber())) {
            throw new ClientException("优惠券不属于当前店铺");
        }
        if (!Objects.equals(couponTemplate.getStatus(), ACTIVE_TEMPLATE_STATUS) || Objects.equals(couponTemplate.getDelFlag(), DELETED_FLAG)) {
            throw new ClientException("优惠券当前不可用");
        }
        if (couponTemplate.getStock() != null && couponTemplate.getStock() <= 0) {
            throw new ClientException("优惠券库存不足");
        }
        validateTimeRange(couponTemplate.getValidStartTime(), couponTemplate.getValidEndTime(), "优惠券已过期或未到可用时间");
    }

    private void validateTimeRange(Date validStartTime, Date validEndTime, String message) {
        Date now = new Date();
        if (validStartTime != null && now.before(validStartTime)) {
            throw new ClientException(message);
        }
        if (validEndTime != null && now.after(validEndTime)) {
            throw new ClientException(message);
        }
    }

    private BigDecimal resolveCalculationAmount(ApplyCouponReqDTO requestParam, CouponTemplateDO couponTemplate) {
        if (!isGoodsCoupon(couponTemplate)) {
            return requestParam.getOrderAmount();
        }
        if (!hasText(couponTemplate.getGoods()) || requestParam.getGoodsList() == null) {
            throw new ClientException("商品券不匹配当前订单商品");
        }
        return requestParam.getGoodsList().stream()
                .filter(each -> Objects.equals(each.getGoodsNumber(), couponTemplate.getGoods()))
                .map(QueryCouponGoodsReqDTO::getGoodsAmount)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new ClientException("商品券不匹配当前订单商品"));
    }

    private boolean isGoodsCoupon(CouponTemplateDO couponTemplate) {
        return Objects.equals(couponTemplate.getTarget(), GOODS_COUPON_TARGET) || hasText(couponTemplate.getGoods());
    }

    private CouponTemplateDO buildCalculationCoupon(CouponTemplateDO couponTemplate) {
        JSONObject consumeRule = Optional.ofNullable(JSON.parseObject(couponTemplate.getConsumeRule()))
                .orElseGet(JSONObject::new);
        Map<String, Object> additionalParams = new HashMap<>();
        switch (couponTemplate.getType()) {
            case 0:
                additionalParams.put("discountAmount", getRuleInteger(consumeRule, "maximumDiscountAmount", "discountAmount"));
                break;
            case 1:
                additionalParams.put("thresholdAmount", getRuleInteger(consumeRule, "termsOfUse", "thresholdAmount"));
                additionalParams.put("discountAmount", getRuleInteger(consumeRule, "maximumDiscountAmount", "discountAmount"));
                break;
            case 2:
                additionalParams.put("discountRate", consumeRule.getDouble("discountRate"));
                break;
            default:
                throw new ClientException("无效的优惠券类型");
        }
        return CouponFactory.createCoupon(couponTemplate, additionalParams);
    }

    private Integer getRuleInteger(JSONObject consumeRule, String primaryKey, String fallbackKey) {
        BigDecimal value = Optional.ofNullable(consumeRule.getBigDecimal(primaryKey))
                .orElse(consumeRule.getBigDecimal(fallbackKey));
        return value == null ? null : value.intValue();
    }

    private BigDecimal capByMaximumDiscount(CouponTemplateDO couponTemplate, BigDecimal couponAmount) {
        JSONObject consumeRule = Optional.ofNullable(JSON.parseObject(couponTemplate.getConsumeRule()))
                .orElseGet(JSONObject::new);
        BigDecimal maximumDiscountAmount = consumeRule.getBigDecimal("maximumDiscountAmount");
        if (maximumDiscountAmount != null && couponAmount != null && couponAmount.compareTo(maximumDiscountAmount) > 0) {
            return maximumDiscountAmount;
        }
        return couponAmount;
    }

    private void validateCouponAmount(BigDecimal couponAmount) {
        if (couponAmount == null || couponAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ClientException("优惠券不满足使用条件");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
