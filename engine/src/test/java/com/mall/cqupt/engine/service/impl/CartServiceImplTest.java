package com.mall.cqupt.engine.service.impl;

import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.context.UserInfoDTO;
import com.mall.cqupt.engine.dao.entity.CartDO;
import com.mall.cqupt.engine.dao.mapper.CartMapper;
import com.mall.cqupt.engine.dto.req.CartAddReqDTO;
import com.mall.cqupt.engine.dto.req.CartUpdateQuantityReqDTO;
import com.mall.cqupt.engine.dto.resp.CartSummaryRespDTO;
import com.mall.cqupt.framework.exception.ClientException;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartMapper cartMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartServiceImpl(cartMapper, stringRedisTemplate);
        UserContext.setUser(UserInfoDTO.builder().userId("42").username("buyer").build());
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void addToCartInsertsNewItemWithDefaultQuantity() {
        CartAddReqDTO request = new CartAddReqDTO();
        request.setGoodsId(1001L);
        request.setShopNumber(2001L);
        request.setQuantity(0);
        when(cartMapper.selectOne(any())).thenReturn(null);
        when(cartMapper.selectCount(any())).thenReturn(0L);

        cartService.addToCart(request);

        ArgumentCaptor<CartDO> captor = ArgumentCaptor.forClass(CartDO.class);
        verify(cartMapper).insert(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals(1001L, captor.getValue().getGoodsId());
        assertEquals(1, captor.getValue().getQuantity());
        assertEquals(1, captor.getValue().getSelected());
        verify(stringRedisTemplate).delete("one-coupon_engine:cart:42");
    }

    @Test
    void addToCartUpdatesExistingItemQuantity() {
        CartAddReqDTO request = new CartAddReqDTO();
        request.setGoodsId(1001L);
        request.setShopNumber(2001L);
        request.setQuantity(3);
        Date createTime = new Date();
        when(cartMapper.selectOne(any())).thenReturn(CartDO.builder()
                .id(1L)
                .userId(42L)
                .goodsId(1001L)
                .shopNumber(2001L)
                .quantity(2)
                .selected(1)
                .createTime(createTime)
                .delFlag(0)
                .build());

        cartService.addToCart(request);

        ArgumentCaptor<CartDO> captor = ArgumentCaptor.forClass(CartDO.class);
        verify(cartMapper).update(captor.capture(), any());
        assertEquals(5, captor.getValue().getQuantity());
        assertEquals(1, captor.getValue().getSelected());
        assertEquals(createTime, captor.getValue().getCreateTime());
        verify(stringRedisTemplate).delete("one-coupon_engine:cart:42");
    }

    @Test
    void updateQuantityRejectsQuantityOverLimit() {
        CartUpdateQuantityReqDTO request = new CartUpdateQuantityReqDTO();
        request.setCartId(1L);
        request.setQuantity(1000);

        assertThrows(ClientException.class, () -> cartService.updateQuantity(request));
    }

    @Test
    void getCartSummaryCalculatesSelectedAndTotalAmounts() {
        when(cartMapper.selectList(any())).thenReturn(List.of(
                cart(1L, 1001L, 2, 1),
                cart(2L, 1002L, 1, 0),
                cart(3L, 1003L, 4, 1)
        ));
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("one-coupon_merchant-admin:goods:1001"))
                .thenReturn(goods("keyboard", "10.00", "12.00", 1, 20));
        when(hashOperations.entries("one-coupon_merchant-admin:goods:1002"))
                .thenReturn(goods("mouse", "5.00", "5.00", 1, 10));
        when(hashOperations.entries("one-coupon_merchant-admin:goods:1003"))
                .thenReturn(goods("offline", "99.00", "120.00", 0, 0));

        CartSummaryRespDTO summary = cartService.getCartSummary();

        assertEquals(3, summary.getItems().size());
        assertEquals(3, summary.getTotalCount());
        assertEquals(2, summary.getSelectedCount());
        assertEquals(0, new BigDecimal("25.00").compareTo(summary.getTotalAmount()));
        assertEquals(0, new BigDecimal("20.00").compareTo(summary.getSelectedAmount()));
        assertEquals(0, new BigDecimal("4.00").compareTo(summary.getSavedAmount()));
        assertEquals(0, new BigDecimal("1.20").compareTo(summary.getTaxAmount()));
        assertEquals(0, new BigDecimal("21.20").compareTo(summary.getPayableAmount()));
    }

    private CartDO cart(Long id, Long goodsId, Integer quantity, Integer selected) {
        return CartDO.builder()
                .id(id)
                .userId(42L)
                .goodsId(goodsId)
                .shopNumber(2001L)
                .quantity(quantity)
                .selected(selected)
                .createTime(new Date())
                .delFlag(0)
                .build();
    }

    private Map<Object, Object> goods(String name, String price, String originalPrice, Integer status, Integer stock) {
        Map<Object, Object> goods = new LinkedHashMap<>();
        goods.put("name", name);
        goods.put("mainImage", name + ".png");
        goods.put("price", price);
        goods.put("originalPrice", originalPrice);
        goods.put("status", status);
        goods.put("stock", stock);
        return goods;
    }
}
