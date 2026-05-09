package com.cqupt.settlement.service.impl;

import com.cqupt.settlement.dao.entity.CouponTemplateDO;
import com.cqupt.settlement.dao.entity.UserCouponDO;
import com.cqupt.settlement.dao.mapper.CouponTemplateMapper;
import com.cqupt.settlement.dao.mapper.UserCouponMapper;
import com.cqupt.settlement.dto.req.ApplyCouponReqDTO;
import com.cqupt.settlement.dto.req.QueryCouponGoodsReqDTO;
import com.cqupt.settlement.dto.resp.ApplyCouponRespDTO;
import com.cqupt.settlement.service.CouponCalculationService;
import com.mall.cqupt.framework.exception.ClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponApplyServiceImplTest {

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    @Mock
    private CouponCalculationService couponCalculationService;

    @InjectMocks
    private CouponApplyServiceImpl couponApplyService;

    @Test
    void applySelectedCouponAppliesAvailableShopCoupon() {
        ApplyCouponReqDTO request = baseRequest(new BigDecimal("100.00"));
        UserCouponDO userCoupon = validUserCoupon(20001L);
        CouponTemplateDO template = shopCoupon(20001L, 1, null);
        when(userCouponMapper.selectOne(any())).thenReturn(userCoupon);
        when(couponTemplateMapper.selectById(20001L)).thenReturn(template);
        when(couponCalculationService.calculateDiscount(any(CouponTemplateDO.class), eq(new BigDecimal("100.00"))))
                .thenReturn(new BigDecimal("30.00"));

        ApplyCouponRespDTO resp = couponApplyService.applySelectedCoupon(request, 20001L);

        assertEquals(90001L, resp.getOrderId());
        assertEquals(0, new BigDecimal("100.00").compareTo(resp.getOriginalAmount()));
        assertEquals(0, new BigDecimal("70.00").compareTo(resp.getFinalAmount()));
        assertEquals(20001L, resp.getAppliedCouponId());
    }

    @Test
    void applySelectedCouponRejectsCouponNotOwnedByUser() {
        when(userCouponMapper.selectOne(any())).thenReturn(null);

        assertThrows(ClientException.class,
                () -> couponApplyService.applySelectedCoupon(baseRequest(new BigDecimal("100.00")), 20001L));

        verify(couponTemplateMapper, never()).selectById(any());
    }

    @Test
    void applySelectedCouponRejectsExpiredUserCoupon() {
        UserCouponDO userCoupon = validUserCoupon(20001L);
        userCoupon.setValidEndTime(new Date(System.currentTimeMillis() - 1000));
        when(userCouponMapper.selectOne(any())).thenReturn(userCoupon);

        assertThrows(ClientException.class,
                () -> couponApplyService.applySelectedCoupon(baseRequest(new BigDecimal("100.00")), 20001L));

        verify(couponTemplateMapper, never()).selectById(any());
    }

    @Test
    void applySelectedCouponRejectsUnavailableCouponTemplate() {
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon(20001L));
        CouponTemplateDO template = shopCoupon(20001L, 1, null);
        template.setStatus(1);
        when(couponTemplateMapper.selectById(20001L)).thenReturn(template);

        assertThrows(ClientException.class,
                () -> couponApplyService.applySelectedCoupon(baseRequest(new BigDecimal("100.00")), 20001L));

        verify(couponCalculationService, never()).calculateDiscount(any(), any());
    }

    @Test
    void applySelectedCouponRejectsGoodsCouponWhenGoodsNotMatched() {
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon(20001L));
        when(couponTemplateMapper.selectById(20001L)).thenReturn(shopCoupon(20001L, 0, "sku-missing"));

        assertThrows(ClientException.class,
                () -> couponApplyService.applySelectedCoupon(baseRequest(new BigDecimal("100.00")), 20001L));

        verify(couponCalculationService, never()).calculateDiscount(any(), any());
    }

    @Test
    void applySelectedCouponCalculatesGoodsCouponDiscountByMatchedGoodsAmount() {
        ApplyCouponReqDTO request = baseRequest(new BigDecimal("100.00"));
        CouponTemplateDO template = shopCoupon(20001L, 0, "sku-1");
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon(20001L));
        when(couponTemplateMapper.selectById(20001L)).thenReturn(template);
        when(couponCalculationService.calculateDiscount(any(CouponTemplateDO.class), eq(new BigDecimal("40.00"))))
                .thenReturn(new BigDecimal("12.00"));

        ApplyCouponRespDTO resp = couponApplyService.applySelectedCoupon(request, 20001L);

        assertEquals(0, new BigDecimal("88.00").compareTo(resp.getFinalAmount()));
    }

    @Test
    void applySelectedCouponNeverReturnsNegativeFinalAmount() {
        ApplyCouponReqDTO request = baseRequest(new BigDecimal("20.00"));
        CouponTemplateDO template = shopCoupon(20001L, 1, null);
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon(20001L));
        when(couponTemplateMapper.selectById(20001L)).thenReturn(template);
        when(couponCalculationService.calculateDiscount(any(CouponTemplateDO.class), eq(new BigDecimal("20.00"))))
                .thenReturn(new BigDecimal("50.00"));

        ApplyCouponRespDTO resp = couponApplyService.applySelectedCoupon(request, 20001L);

        assertEquals(0, BigDecimal.ZERO.compareTo(resp.getFinalAmount()));
    }

    private ApplyCouponReqDTO baseRequest(BigDecimal orderAmount) {
        QueryCouponGoodsReqDTO goods = new QueryCouponGoodsReqDTO();
        goods.setGoodsNumber("sku-1");
        goods.setGoodsAmount(new BigDecimal("40.00"));

        ApplyCouponReqDTO request = new ApplyCouponReqDTO();
        request.setUserId(10L);
        request.setShopNumber(10001L);
        request.setOrderId(90001L);
        request.setOrderAmount(orderAmount);
        request.setGoodsList(List.of(goods));
        return request;
    }

    private UserCouponDO validUserCoupon(Long couponTemplateId) {
        Date now = new Date();
        return UserCouponDO.builder()
                .id(30001L)
                .userId(10L)
                .couponTemplateId(couponTemplateId)
                .validStartTime(new Date(now.getTime() - 60_000))
                .validEndTime(new Date(now.getTime() + 60_000))
                .status(0)
                .delFlag(0)
                .build();
    }

    private CouponTemplateDO shopCoupon(Long couponTemplateId, Integer target, String goods) {
        Date now = new Date();
        return CouponTemplateDO.builder()
                .id(couponTemplateId)
                .shopNumber(10001L)
                .target(target)
                .goods(goods)
                .type(0)
                .validStartTime(new Date(now.getTime() - 60_000))
                .validEndTime(new Date(now.getTime() + 60_000))
                .status(0)
                .delFlag(0)
                .build();
    }
}
