package com.mall.cqupt.merchant.admin.template;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamBaseVerifyChainFilter;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamNotNullChainFilter;
import com.mall.cqupt.merchant.admin.service.handler.filter.CouponTemplateCreateParamVerifyChainFilter;
import com.mall.cqupt.merchant.admin.service.impl.CouponTemplateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponTemplateServiceImplTest {

    @Mock
    private CouponTemplateMapper couponTemplateMapper;

    private MerchantAdminChainContext merchantAdminChainContext;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private CouponTemplateServiceImpl couponTemplateService;

    @BeforeEach
    void setUp() throws Exception {
        merchantAdminChainContext = new MerchantAdminChainContext();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBeansOfType(any())).thenReturn(Map.of(
                "notNullFilter", new CouponTemplateCreateParamNotNullChainFilter(),
                "baseVerifyFilter", new CouponTemplateCreateParamBaseVerifyChainFilter(),
                "verifyFilter", new CouponTemplateCreateParamVerifyChainFilter()
        ));
        merchantAdminChainContext.setApplicationContext(applicationContext);
        merchantAdminChainContext.run();
        couponTemplateService = new CouponTemplateServiceImpl(
                couponTemplateMapper,
                merchantAdminChainContext,
                stringRedisTemplate
        );
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void platformCreateCouponTemplateNormalizesSourceAndShopNumber() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("1")
                .username("platform")
                .roleType(UserRoleEnum.PLATFORM.getType())
                .build());
        CouponTemplateSaveReqDTO request = validCouponRequest();
        request.setSource(1);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        couponTemplateService.createCouponTemplate(request);

        ArgumentCaptor<CouponTemplateDO> captor = ArgumentCaptor.forClass(CouponTemplateDO.class);
        verify(couponTemplateMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getShopNumber());
        assertEquals(1, captor.getValue().getSource());
        assertEquals(CouponTemplateStatusEnum.ACTIVE.getStatus(), captor.getValue().getStatus());
        verify(hashOperations).putAll(eq("one-coupon_engine:template:null"), any());
    }

    @Test
    void merchantCannotCreatePlatformCouponTemplate() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("2")
                .username("merchant")
                .roleType(UserRoleEnum.MERCHANT.getType())
                .shopNumber(10001L)
                .build());
        CouponTemplateSaveReqDTO request = validCouponRequest();
        request.setSource(1);

        assertThrows(ClientException.class, () -> couponTemplateService.createCouponTemplate(request));
    }

    @Test
    void terminateCouponTemplateRejectsEndedTemplate() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("2")
                .username("merchant")
                .roleType(UserRoleEnum.MERCHANT.getType())
                .shopNumber(10001L)
                .build());
        when(couponTemplateMapper.selectOne(any())).thenReturn(CouponTemplateDO.builder()
                .id(100L)
                .shopNumber(10001L)
                .source(0)
                .status(CouponTemplateStatusEnum.ENDED.getStatus())
                .build());

        assertThrows(ClientException.class, () -> couponTemplateService.terminateCouponTemplate("100"));
    }

    @Test
    void terminateCouponTemplateUpdatesDatabaseAndCache() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("2")
                .username("merchant")
                .roleType(UserRoleEnum.MERCHANT.getType())
                .shopNumber(10001L)
                .build());
        when(couponTemplateMapper.selectOne(any())).thenReturn(CouponTemplateDO.builder()
                .id(100L)
                .shopNumber(10001L)
                .source(0)
                .status(CouponTemplateStatusEnum.ACTIVE.getStatus())
                .build());
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        couponTemplateService.terminateCouponTemplate("100");

        ArgumentCaptor<CouponTemplateDO> captor = ArgumentCaptor.forClass(CouponTemplateDO.class);
        verify(couponTemplateMapper).update(captor.capture(), any());
        assertEquals(CouponTemplateStatusEnum.ENDED.getStatus(), captor.getValue().getStatus());
        verify(hashOperations).put("one-coupon_engine:template:100", "status", "1");
    }

    @Test
    void increaseNumberCouponTemplateThrowsWhenDatabaseUpdateFails() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("2")
                .username("merchant")
                .roleType(UserRoleEnum.MERCHANT.getType())
                .shopNumber(10001L)
                .build());
        when(couponTemplateMapper.selectOne(any())).thenReturn(CouponTemplateDO.builder()
                .id(100L)
                .shopNumber(10001L)
                .source(0)
                .status(CouponTemplateStatusEnum.ACTIVE.getStatus())
                .build());
        when(couponTemplateMapper.increaseNumberCouponTemplate(10001L, "100", 5)).thenReturn(0);

        assertThrows(ServiceException.class, () -> couponTemplateService.increaseNumberCouponTemplate(increaseRequest()));
    }

    @Test
    void findCouponTemplateByIdMapsDatabaseEntity() {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("2")
                .username("merchant")
                .roleType(UserRoleEnum.MERCHANT.getType())
                .shopNumber(10001L)
                .build());
        when(couponTemplateMapper.selectOne(any())).thenReturn(CouponTemplateDO.builder()
                .id(100L)
                .shopNumber(10001L)
                .name("coupon")
                .source(0)
                .target(1)
                .type(1)
                .stock(50)
                .status(0)
                .build());

        CouponTemplateQueryRespDTO resp = couponTemplateService.findCouponTemplateById("100");

        assertEquals(100L, resp.getId());
        assertEquals("coupon", resp.getName());
        assertEquals(50, resp.getStock());
    }

    private CouponTemplateSaveReqDTO validCouponRequest() {
        CouponTemplateSaveReqDTO request = new CouponTemplateSaveReqDTO();
        request.setName("coupon");
        request.setSource(0);
        request.setTarget(1);
        request.setType(1);
        request.setValidStartTime(new Date(System.currentTimeMillis() + 60_000L));
        request.setValidEndTime(new Date(System.currentTimeMillis() + 3_600_000L));
        request.setStock(100);
        request.setReceiveRule("{\"limitPerPerson\":1}");
        request.setConsumeRule("{\"termsOfUse\":100,\"maximumDiscountAmount\":20}");
        return request;
    }

    private CouponTemplateNumberReqDTO increaseRequest() {
        CouponTemplateNumberReqDTO request = new CouponTemplateNumberReqDTO();
        request.setCouponTemplateId("100");
        request.setNumber(5);
        return request;
    }
}
