package com.mall.cqupt.engine.service.impl;

import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.context.UserInfoDTO;
import com.mall.cqupt.engine.common.enums.UserCouponStatusEnum;
import com.mall.cqupt.engine.dao.entity.CouponSettlementDO;
import com.mall.cqupt.engine.dao.entity.UserCouponDO;
import com.mall.cqupt.engine.dao.mapper.CouponSettlementMapper;
import com.mall.cqupt.engine.dao.mapper.UserCouponMapper;
import com.mall.cqupt.engine.dto.req.CouponCreatePaymentGoodsReqDTO;
import com.mall.cqupt.engine.dto.req.CouponCreatePaymentReqDTO;
import com.mall.cqupt.engine.dto.req.CouponProcessPaymentReqDTO;
import com.mall.cqupt.engine.dto.req.CouponProcessRefundReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponPayServiceImplTest {

    @Mock
    private CouponTemplateService couponTemplateService;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private CouponSettlementMapper couponSettlementMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private TransactionTemplate transactionTemplate;

    private CouponPayServiceImpl couponPayService;

    @BeforeEach
    void setUp() {
        couponPayService = new CouponPayServiceImpl(
                couponTemplateService,
                userCouponMapper,
                couponSettlementMapper,
                redissonClient,
                transactionTemplate
        );
        UserContext.setUser(UserInfoDTO.builder().userId("42").username("buyer").build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void createPaymentRecordRejectsWhenLockIsBusy() {
        CouponCreatePaymentReqDTO request = paymentRequest();
        when(redissonClient.getLock("one-coupon_engine:lock:coupon-settlement:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(false);

        assertThrows(ClientException.class, () -> couponPayService.createPaymentRecord(request));
    }

    @Test
    void createPaymentRecordRejectsAmountMismatch() {
        CouponCreatePaymentReqDTO request = paymentRequest();
        request.setPayableAmount(new BigDecimal("85.00"));
        when(redissonClient.getLock("one-coupon_engine:lock:coupon-settlement:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(couponSettlementMapper.selectOne(any())).thenReturn(null);
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon());
        when(couponTemplateService.findCouponTemplate(any())).thenReturn(storeCouponTemplate());

        assertThrows(ClientException.class, () -> couponPayService.createPaymentRecord(request));
        verify(lock).unlock();
    }

    @Test
    void createPaymentRecordCreatesSettlementAndLocksUserCoupon() {
        CouponCreatePaymentReqDTO request = paymentRequest();
        when(redissonClient.getLock("one-coupon_engine:lock:coupon-settlement:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        when(couponSettlementMapper.selectOne(any())).thenReturn(null);
        when(userCouponMapper.selectOne(any())).thenReturn(validUserCoupon());
        when(couponTemplateService.findCouponTemplate(any())).thenReturn(storeCouponTemplate());
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer>getArgument(0)
                    .accept(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        couponPayService.createPaymentRecord(request);

        ArgumentCaptor<CouponSettlementDO> settlementCaptor = ArgumentCaptor.forClass(CouponSettlementDO.class);
        ArgumentCaptor<UserCouponDO> userCouponCaptor = ArgumentCaptor.forClass(UserCouponDO.class);
        verify(couponSettlementMapper).insert(settlementCaptor.capture());
        verify(userCouponMapper).update(userCouponCaptor.capture(), any());
        assertEquals(9001L, settlementCaptor.getValue().getOrderId());
        assertEquals(1L, settlementCaptor.getValue().getCouponId());
        assertEquals(42L, settlementCaptor.getValue().getUserId());
        assertEquals(0, settlementCaptor.getValue().getStatus());
        assertEquals(UserCouponStatusEnum.LOCKING.getCode(), userCouponCaptor.getValue().getStatus());
        verify(lock).unlock();
    }

    @Test
    void processPaymentThrowsWhenSettlementUpdateFails() {
        CouponProcessPaymentReqDTO request = new CouponProcessPaymentReqDTO();
        request.setCouponId(1L);
        when(redissonClient.getLock("one-coupon_engine:lock:coupon-settlement:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer>getArgument(0)
                    .accept(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(couponSettlementMapper.update(any(), any())).thenReturn(0);

        assertThrows(ServiceException.class, () -> couponPayService.processPayment(request));
        verify(lock).unlock();
    }

    @Test
    void processRefundRestoresUserCouponWhenUpdatesSucceed() {
        CouponProcessRefundReqDTO request = new CouponProcessRefundReqDTO();
        request.setCouponId(1L);
        when(redissonClient.getLock("one-coupon_engine:lock:coupon-settlement:1")).thenReturn(lock);
        when(lock.tryLock()).thenReturn(true);
        doAnswer(invocation -> {
            invocation.<java.util.function.Consumer>getArgument(0)
                    .accept(org.mockito.Mockito.mock(org.springframework.transaction.TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(couponSettlementMapper.update(any(), any())).thenReturn(1);
        when(userCouponMapper.update(any(), any())).thenReturn(1);

        couponPayService.processRefund(request);

        ArgumentCaptor<UserCouponDO> userCouponCaptor = ArgumentCaptor.forClass(UserCouponDO.class);
        verify(userCouponMapper).update(userCouponCaptor.capture(), any());
        assertEquals(UserCouponStatusEnum.UNUSED.getCode(), userCouponCaptor.getValue().getStatus());
        verify(lock).unlock();
    }

    private CouponCreatePaymentReqDTO paymentRequest() {
        CouponCreatePaymentReqDTO request = new CouponCreatePaymentReqDTO();
        request.setCouponId(1L);
        request.setOrderId(9001L);
        request.setOrderAmount(new BigDecimal("100.00"));
        request.setPayableAmount(new BigDecimal("80.00"));
        request.setShopNumber("10001");
        request.setGoodsList(List.of(goods("sku-1", "100.00", "80.00")));
        return request;
    }

    private CouponCreatePaymentGoodsReqDTO goods(String goodsNumber, String amount, String payableAmount) {
        CouponCreatePaymentGoodsReqDTO goods = new CouponCreatePaymentGoodsReqDTO();
        goods.setGoodsNumber(goodsNumber);
        goods.setGoodsAmount(new BigDecimal(amount));
        goods.setGoodsPayableAmount(new BigDecimal(payableAmount));
        return goods;
    }

    private UserCouponDO validUserCoupon() {
        return UserCouponDO.builder()
                .id(1L)
                .userId(42L)
                .couponTemplateId(100L)
                .status(UserCouponStatusEnum.UNUSED.getCode())
                .validEndTime(new Date(System.currentTimeMillis() + 60_000L))
                .build();
    }

    private CouponTemplateQueryRespDTO storeCouponTemplate() {
        CouponTemplateQueryRespDTO template = new CouponTemplateQueryRespDTO();
        template.setId("100");
        template.setShopNumber("10001");
        template.setSource(0);
        template.setTarget(1);
        template.setType(1);
        template.setConsumeRule("{\"termsOfUse\":50,\"maximumDiscountAmount\":20}");
        return template;
    }
}
