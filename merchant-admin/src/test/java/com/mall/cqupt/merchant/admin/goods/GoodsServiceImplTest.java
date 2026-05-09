package com.mall.cqupt.merchant.admin.goods;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.GoodsCategoryStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.GoodsStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsCategoryDO;
import com.mall.cqupt.merchant.admin.dao.entity.GoodsDO;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsAttributeMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsAttributeValueMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsCategoryMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsImageMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.GoodsMapper;
import com.mall.cqupt.merchant.admin.dao.mapper.UserMapper;
import com.mall.cqupt.merchant.admin.dto.req.GoodsSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsStockReqDTO;
import com.mall.cqupt.merchant.admin.service.impl.GoodsServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoodsServiceImplTest {

    @Mock
    private GoodsMapper goodsMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @Mock
    private GoodsImageMapper goodsImageMapper;

    @Mock
    private GoodsAttributeMapper goodsAttributeMapper;

    @Mock
    private GoodsAttributeValueMapper goodsAttributeValueMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private GoodsServiceImpl goodsService;

    @BeforeEach
    void setUp() {
        goodsService = new GoodsServiceImpl(
                goodsMapper,
                goodsCategoryMapper,
                goodsImageMapper,
                goodsAttributeMapper,
                goodsAttributeValueMapper,
                userMapper,
                stringRedisTemplate
        );
        UserContext.setUser(UserInfoDTO.builder()
                .userId("1")
                .username("platform")
                .roleType(UserRoleEnum.PLATFORM.getType())
                .build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void createGoodsRejectsDisabledCategory() {
        when(goodsCategoryMapper.selectById(1L)).thenReturn(GoodsCategoryDO.builder()
                .id(1L)
                .status(GoodsCategoryStatusEnum.DISABLED.getStatus())
                .delFlag(0)
                .build());

        assertThrows(ClientException.class, () -> goodsService.createGoods(validGoodsRequest()));
    }

    @Test
    void createGoodsInsertsOffShelfGoodsAndWarmsCache() {
        when(goodsCategoryMapper.selectById(1L)).thenReturn(GoodsCategoryDO.builder()
                .id(1L)
                .status(GoodsCategoryStatusEnum.ENABLED.getStatus())
                .delFlag(0)
                .build());
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        goodsService.createGoods(validGoodsRequest());

        ArgumentCaptor<GoodsDO> captor = ArgumentCaptor.forClass(GoodsDO.class);
        verify(goodsMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getShopNumber());
        assertEquals(GoodsStatusEnum.OFF_SHELF.getStatus(), captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getSales());
        verify(hashOperations).putAll(eq("one-coupon_merchant-admin:goods:null"), any());
        verify(stringRedisTemplate).expire("one-coupon_merchant-admin:goods:null", 24, TimeUnit.HOURS);
    }

    @Test
    void updateGoodsStatusRejectsIllegalStatus() {
        assertThrows(ClientException.class, () -> goodsService.updateGoodsStatus("1", 99));
    }

    @Test
    void updateGoodsStatusUpdatesDatabaseAndCache() {
        when(goodsMapper.selectOne(any())).thenReturn(GoodsDO.builder()
                .id(1L)
                .shopNumber(0L)
                .status(GoodsStatusEnum.OFF_SHELF.getStatus())
                .delFlag(0)
                .build());
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        goodsService.updateGoodsStatus("1", GoodsStatusEnum.ON_SHELF.getStatus());

        ArgumentCaptor<GoodsDO> captor = ArgumentCaptor.forClass(GoodsDO.class);
        verify(goodsMapper).update(captor.capture(), any());
        assertEquals(GoodsStatusEnum.ON_SHELF.getStatus(), captor.getValue().getStatus());
        verify(hashOperations).put("one-coupon_merchant-admin:goods:1", "status", "1");
    }

    @Test
    void adjustStockRejectsZeroQuantity() {
        GoodsStockReqDTO request = new GoodsStockReqDTO();
        request.setGoodsId(1L);
        request.setQuantity(0);

        assertThrows(ClientException.class, () -> goodsService.adjustStock(request));
    }

    @Test
    void adjustStockThrowsWhenDecreaseFails() {
        GoodsStockReqDTO request = new GoodsStockReqDTO();
        request.setGoodsId(1L);
        request.setQuantity(-5);
        when(goodsMapper.decreaseStock(0L, 1L, 5)).thenReturn(0);

        assertThrows(ServiceException.class, () -> goodsService.adjustStock(request));
    }

    @Test
    void deleteGoodsRejectsOnShelfGoods() {
        when(goodsMapper.selectOne(any())).thenReturn(GoodsDO.builder()
                .id(1L)
                .shopNumber(0L)
                .status(GoodsStatusEnum.ON_SHELF.getStatus())
                .delFlag(0)
                .build());

        assertThrows(ClientException.class, () -> goodsService.deleteGoods("1"));
    }

    private GoodsSaveReqDTO validGoodsRequest() {
        GoodsSaveReqDTO request = new GoodsSaveReqDTO();
        request.setName("keyboard");
        request.setCategoryId(1L);
        request.setPrice(new BigDecimal("99.00"));
        request.setOriginalPrice(new BigDecimal("129.00"));
        request.setStock(100);
        request.setUnit("piece");
        request.setSortOrder(1);
        return request;
    }
}
