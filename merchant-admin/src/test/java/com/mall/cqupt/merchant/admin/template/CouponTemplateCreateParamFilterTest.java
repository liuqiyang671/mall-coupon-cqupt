package com.mall.cqupt.merchant.admin.template;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.merchant.admin.common.enums.DiscountTargetEnum;
import com.mall.cqupt.merchant.admin.common.enums.DiscountTypeEnum;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamBaseVerifyChainFilter;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamNotNullChainFilter;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamVerifyChainFilter;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CouponTemplateCreateParamFilterTest {

    private final CouponTemplateCreateParamNotNullChainFilter notNullFilter =
            new CouponTemplateCreateParamNotNullChainFilter();
    private final CouponTemplateCreateParamBaseVerifyChainFilter baseVerifyFilter =
            new CouponTemplateCreateParamBaseVerifyChainFilter();
    private final CouponTemplateCreateParamVerifyChainFilter verifyFilter =
            new CouponTemplateCreateParamVerifyChainFilter();

    @Test
    void notNullFilterAcceptsCompleteRequest() {
        assertDoesNotThrow(() -> notNullFilter.handler(validProductThresholdRequest()));
        assertEquals(0, notNullFilter.getOrder());
    }

    @Test
    void notNullFilterRejectsBlankName() {
        CouponTemplateSaveReqDTO request = validProductThresholdRequest();
        request.setName("");

        assertThrows(ClientException.class, () -> notNullFilter.handler(request));
    }

    @Test
    void baseVerifyRejectsAllStoreCouponWithSpecificGoods() {
        CouponTemplateSaveReqDTO request = validProductThresholdRequest();
        request.setTarget(DiscountTargetEnum.ALL_STORE_GENERAL.getType());
        request.setGoods("sku-1");

        assertThrows(ClientException.class, () -> baseVerifyFilter.handler(request));
    }

    @Test
    void baseVerifyRejectsProductSpecificCouponWithoutGoods() {
        CouponTemplateSaveReqDTO request = validProductThresholdRequest();
        request.setGoods("");

        assertThrows(ClientException.class, () -> baseVerifyFilter.handler(request));
    }

    @Test
    void baseVerifyAcceptsValidBasicRules() {
        assertDoesNotThrow(() -> baseVerifyFilter.handler(validProductThresholdRequest()));
        assertEquals(10, baseVerifyFilter.getOrder());
    }

    @Test
    void verifyFilterRejectsThresholdCouponWhenDiscountIsNotLessThanThreshold() {
        CouponTemplateSaveReqDTO request = validProductThresholdRequest();
        request.setConsumeRule("{\"thresholdAmount\":20,\"discountAmount\":20}");

        assertThrows(ClientException.class, () -> verifyFilter.handler(request));
    }

    @Test
    void verifyFilterRejectsDiscountCouponWithoutMaxDiscountLimit() {
        CouponTemplateSaveReqDTO request = validProductThresholdRequest();
        request.setType(DiscountTypeEnum.DISCOUNT_COUPON.getType());
        request.setConsumeRule("{\"discountRate\":8.8}");

        assertThrows(ClientException.class, () -> verifyFilter.handler(request));
    }

    @Test
    void verifyFilterAcceptsValidProductSpecificThresholdCoupon() {
        assertDoesNotThrow(() -> verifyFilter.handler(validProductThresholdRequest()));
        assertEquals(20, verifyFilter.getOrder());
    }

    private CouponTemplateSaveReqDTO validProductThresholdRequest() {
        CouponTemplateSaveReqDTO request = new CouponTemplateSaveReqDTO();
        request.setName("coupon");
        request.setSource(0);
        request.setTarget(DiscountTargetEnum.PRODUCT_SPECIFIC.getType());
        request.setGoods("sku-1");
        request.setType(DiscountTypeEnum.THRESHOLD_DISCOUNT.getType());
        request.setValidStartTime(new Date(System.currentTimeMillis() + 60_000L));
        request.setValidEndTime(new Date(System.currentTimeMillis() + 3_600_000L));
        request.setStock(100);
        request.setReceiveRule("{\"limitPerPerson\":1}");
        request.setConsumeRule("{\"thresholdAmount\":100,\"discountAmount\":20}");
        return request;
    }
}
