package com.mall.cqupt.engine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.context.UserInfoDTO;
import com.mall.cqupt.engine.dao.entity.UserCouponDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.engine.dao.mapper.UserCouponMapper;
import com.mall.cqupt.engine.dto.req.UserCouponPageQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.UserCouponPageQueryRespDTO;
import com.mall.cqupt.engine.mq.producer.UserCouponDelayCloseProducer;
import com.mall.cqupt.engine.mq.producer.UserCouponRedeemProducer;
import com.mall.cqupt.engine.service.CouponTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceImplTest {

    @Mock
    private CouponTemplateService couponTemplateService;

    @Mock
    private UserCouponMapper userCouponMapper;

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    @Mock
    private UserCouponDelayCloseProducer couponDelayCloseProducer;

    @Mock
    private UserCouponRedeemProducer userCouponRedeemProducer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private UserCouponServiceImpl userCouponService;

    @BeforeEach
    void setUp() {
        userCouponService = new UserCouponServiceImpl(
                couponTemplateService,
                userCouponMapper,
                couponTemplateMapper,
                couponDelayCloseProducer,
                userCouponRedeemProducer,
                stringRedisTemplate,
                transactionTemplate
        );
        UserContext.setUser(UserInfoDTO.builder().userId("42").username("buyer").build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void pageUserCouponMapsCouponAndTemplateCacheFields() {
        UserCouponPageQueryReqDTO request = new UserCouponPageQueryReqDTO();
        request.setCurrent(1);
        request.setSize(10);
        request.setStatus(0);

        Page<UserCouponDO> page = new Page<>(1, 10);
        page.setRecords(List.of(UserCouponDO.builder()
                .id(1L)
                .couponTemplateId(100L)
                .userId(42L)
                .receiveCount(1)
                .source(0)
                .status(0)
                .receiveTime(new Date())
                .validStartTime(new Date())
                .validEndTime(new Date())
                .build()));

        when(userCouponMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("one-coupon_engine:template:100")).thenReturn(templateCache());

        IPage<UserCouponPageQueryRespDTO> resp = userCouponService.pageUserCoupon(request);

        assertEquals(1, resp.getRecords().size());
        UserCouponPageQueryRespDTO coupon = resp.getRecords().get(0);
        assertEquals(1L, coupon.getId());
        assertEquals(100L, coupon.getCouponTemplateId());
        assertEquals("full reduction", coupon.getName());
        assertEquals("10001", coupon.getShopNumber());
        assertEquals(1, coupon.getTarget());
        assertEquals(1, coupon.getType());
        assertEquals("{\"limitPerPerson\":1}", coupon.getReceiveRule());
    }

    private Map<Object, Object> templateCache() {
        Map<Object, Object> cache = new LinkedHashMap<>();
        cache.put("name", "full reduction");
        cache.put("shopNumber", "10001");
        cache.put("target", "1");
        cache.put("goods", "");
        cache.put("type", "1");
        cache.put("receiveRule", "{\"limitPerPerson\":1}");
        cache.put("consumeRule", "{\"termsOfUse\":100,\"maximumDiscountAmount\":20}");
        return cache;
    }
}
