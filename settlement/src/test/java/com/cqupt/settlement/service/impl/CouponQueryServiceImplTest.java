package com.cqupt.settlement.service.impl;

import com.cqupt.settlement.dto.req.QueryCouponGoodsReqDTO;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;
import com.mall.cqupt.framework.config.RedisDistributedProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponQueryServiceImplTest {

    @Mock
    private RedisDistributedProperties redisDistributedProperties;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @InjectMocks
    private CouponQueryServiceImpl couponQueryService;

    @Test
    void listQueryUserCouponsBySyncReturnsEmptyWhenUserHasNoCoupons() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range("one-coupon_engine:user-template-list:10", 0, -1))
                .thenReturn(Collections.emptySet());

        QueryCouponsRespDTO resp = couponQueryService.listQueryUserCouponsBySync(baseRequest());

        assertTrue(resp.getAvailableCoupons().isEmpty());
        assertTrue(resp.getNotAvailableCoupons().isEmpty());
        verify(stringRedisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    void listQueryUserCouponsBySyncClassifiesAndSortsCoupons() {
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.range("one-coupon_engine:user-template-list:10", 0, -1))
                .thenReturn(new LinkedHashSet<>(List.of("100_1", "101_1", "102_1", "103_1", "104_1")));
        when(redisDistributedProperties.getPrefix()).thenReturn("");
        when(stringRedisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(couponTemplates());

        QueryCouponsRespDTO resp = couponQueryService.listQueryUserCouponsBySync(baseRequest());

        assertEquals(3, resp.getAvailableCoupons().size());
        assertEquals("101", resp.getAvailableCoupons().get(0).getId());
        assertEquals("103", resp.getAvailableCoupons().get(1).getId());
        assertEquals("100", resp.getAvailableCoupons().get(2).getId());
        assertEquals(0, new BigDecimal("10").compareTo(resp.getAvailableCoupons().get(0).getCouponAmount()));
        assertEquals(0, new BigDecimal("6.000").compareTo(resp.getAvailableCoupons().get(1).getCouponAmount()));
        assertEquals(2, resp.getNotAvailableCoupons().size());
        assertEquals("102", resp.getNotAvailableCoupons().get(0).getId());
        assertEquals("104", resp.getNotAvailableCoupons().get(1).getId());
    }

    private QueryCouponsReqDTO baseRequest() {
        QueryCouponGoodsReqDTO goods = new QueryCouponGoodsReqDTO();
        goods.setGoodsNumber("sku-1");
        goods.setGoodsAmount(new BigDecimal("30.00"));

        QueryCouponsReqDTO request = new QueryCouponsReqDTO();
        request.setUserId(10L);
        request.setOrderAmount(new BigDecimal("60.00"));
        request.setGoodsList(List.of(goods));
        return request;
    }

    private List<Object> couponTemplates() {
        List<Object> results = new ArrayList<>();
        results.add(coupon("100", 1, null, 0,
                "{\"termsOfUse\":0,\"maximumDiscountAmount\":5}"));
        results.add(coupon("101", 1, null, 1,
                "{\"termsOfUse\":50,\"maximumDiscountAmount\":10}"));
        results.add(coupon("102", 1, null, 1,
                "{\"termsOfUse\":100,\"maximumDiscountAmount\":20}"));
        results.add(coupon("103", 0, "sku-1", 2,
                "{\"termsOfUse\":20,\"maximumDiscountAmount\":50,\"discountRate\":0.2}"));
        results.add(coupon("104", 0, "sku-missing", 1,
                "{\"termsOfUse\":10,\"maximumDiscountAmount\":3}"));
        return results;
    }

    private Map<String, Object> coupon(String id, Integer target, String goods, Integer type, String consumeRule) {
        Map<String, Object> coupon = new LinkedHashMap<>();
        coupon.put("id", id);
        coupon.put("target", target);
        coupon.put("goods", goods);
        coupon.put("type", type);
        coupon.put("consumeRule", consumeRule);
        return coupon;
    }
}
