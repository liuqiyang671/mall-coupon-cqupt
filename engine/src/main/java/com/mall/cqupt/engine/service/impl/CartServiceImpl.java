package com.mall.cqupt.engine.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.dao.entity.CartDO;
import com.mall.cqupt.engine.dao.mapper.CartMapper;
import com.mall.cqupt.engine.dto.req.CartAddReqDTO;
import com.mall.cqupt.engine.dto.req.CartSelectReqDTO;
import com.mall.cqupt.engine.dto.req.CartUpdateQuantityReqDTO;
import com.mall.cqupt.engine.dto.resp.CartItemRespDTO;
import com.mall.cqupt.engine.dto.resp.CartSummaryRespDTO;
import com.mall.cqupt.engine.service.CartService;
import com.mall.cqupt.framework.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, CartDO> implements CartService {

    private final CartMapper cartMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CART_CACHE_KEY = "one-coupon_engine:cart:%s";
    private static final long CACHE_EXPIRE_HOURS = 48;
    private static final BigDecimal TAX_RATE = new BigDecimal("0.06");
    private static final int MAX_QUANTITY = 999;
    private static final int MAX_CART_ITEMS = 50;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(CartAddReqDTO requestParam) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);
        if (requestParam.getGoodsId() == null) {
            throw new ClientException("商品ID不能为空");
        }
        if (requestParam.getQuantity() == null || requestParam.getQuantity() <= 0) {
            requestParam.setQuantity(1);
        }
        if (requestParam.getQuantity() > MAX_QUANTITY) {
            throw new ClientException("单次添加数量不能超过" + MAX_QUANTITY);
        }

        LambdaQueryWrapper<CartDO> queryWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getGoodsId, requestParam.getGoodsId())
                .eq(CartDO::getDelFlag, 0);
        CartDO existingItem = cartMapper.selectOne(queryWrapper);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + requestParam.getQuantity();
            if (newQuantity > MAX_QUANTITY) {
                throw new ClientException("商品数量不能超过" + MAX_QUANTITY);
            }
            CartDO updateDO = CartDO.builder().quantity(newQuantity).build();
            LambdaQueryWrapper<CartDO> updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                    .eq(CartDO::getId, existingItem.getId());
            cartMapper.update(updateDO, updateWrapper);
        } else {
            long cartCount = cartMapper.selectCount(Wrappers.lambdaQuery(CartDO.class)
                    .eq(CartDO::getUserId, userId)
                    .eq(CartDO::getDelFlag, 0));
            if (cartCount >= MAX_CART_ITEMS) {
                throw new ClientException("购物车最多添加" + MAX_CART_ITEMS + "种商品");
            }

            CartDO cartDO = CartDO.builder()
                    .userId(userId)
                    .goodsId(requestParam.getGoodsId())
                    .shopNumber(requestParam.getShopNumber())
                    .quantity(requestParam.getQuantity())
                    .selected(1)
                    .build();
            cartMapper.insert(cartDO);
        }

        deleteCartCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(CartUpdateQuantityReqDTO requestParam) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);
        if (requestParam.getQuantity() == null || requestParam.getQuantity() <= 0) {
            throw new ClientException("商品数量必须大于0");
        }
        if (requestParam.getQuantity() > MAX_QUANTITY) {
            throw new ClientException("商品数量不能超过" + MAX_QUANTITY);
        }

        CartDO existingItem = getCartWithOwnerCheck(userId, requestParam.getCartId());

        CartDO updateDO = CartDO.builder().quantity(requestParam.getQuantity()).build();
        LambdaQueryWrapper<CartDO> updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getId, existingItem.getId())
                .eq(CartDO::getUserId, userId);
        cartMapper.update(updateDO, updateWrapper);

        deleteCartCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long cartId) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);

        getCartWithOwnerCheck(userId, cartId);

        CartDO updateDO = CartDO.builder().delFlag(1).build();
        LambdaQueryWrapper<CartDO> updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getId, cartId)
                .eq(CartDO::getUserId, userId);
        cartMapper.update(updateDO, updateWrapper);

        deleteCartCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItems(List<Long> cartIds) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);
        if (CollUtil.isEmpty(cartIds)) {
            return;
        }

        CartDO updateDO = CartDO.builder().delFlag(1).build();
        LambdaQueryWrapper<CartDO> updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                .in(CartDO::getId, cartIds)
                .eq(CartDO::getUserId, userId);
        cartMapper.update(updateDO, updateWrapper);

        deleteCartCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart() {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);

        CartDO updateDO = CartDO.builder().delFlag(1).build();
        LambdaQueryWrapper<CartDO> updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getDelFlag, 0);
        cartMapper.update(updateDO, updateWrapper);

        deleteCartCache(userId);
    }

    @Override
    public CartSummaryRespDTO getCartSummary() {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);

        List<CartItemRespDTO> items = queryCartItems(userId);
        return calculateSummary(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelected(CartSelectReqDTO requestParam) {
        String userIdStr = UserContext.getUserId();
        if (userIdStr == null) {
            throw new ClientException("用户未登录");
        }
        Long userId = Long.parseLong(userIdStr);
        if (requestParam.getSelected() == null
                || (requestParam.getSelected() != 0 && requestParam.getSelected() != 1)) {
            throw new ClientException("选中状态值不合法");
        }

        CartDO updateDO = CartDO.builder().selected(requestParam.getSelected()).build();
        LambdaQueryWrapper<CartDO> updateWrapper;

        if (CollUtil.isNotEmpty(requestParam.getCartIds())) {
            updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                    .in(CartDO::getId, requestParam.getCartIds())
                    .eq(CartDO::getUserId, userId)
                    .eq(CartDO::getDelFlag, 0);
        } else {
            updateWrapper = Wrappers.lambdaQuery(CartDO.class)
                    .eq(CartDO::getUserId, userId)
                    .eq(CartDO::getDelFlag, 0);
        }
        cartMapper.update(updateDO, updateWrapper);

        deleteCartCache(userId);
    }

    private CartDO getCartWithOwnerCheck(Long userId, Long cartId) {
        LambdaQueryWrapper<CartDO> queryWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getId, cartId)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getDelFlag, 0);
        CartDO cartDO = cartMapper.selectOne(queryWrapper);
        if (cartDO == null) {
            throw new ClientException("购物车项不存在");
        }
        return cartDO;
    }

    private List<CartItemRespDTO> queryCartItems(Long userId) {
        LambdaQueryWrapper<CartDO> queryWrapper = Wrappers.lambdaQuery(CartDO.class)
                .eq(CartDO::getUserId, userId)
                .eq(CartDO::getDelFlag, 0)
                .orderByDesc(CartDO::getCreateTime);
        List<CartDO> cartList = cartMapper.selectList(queryWrapper);

        List<CartItemRespDTO> items = new ArrayList<>();
        for (CartDO cartDO : cartList) {
            CartItemRespDTO item = new CartItemRespDTO();
            item.setId(cartDO.getId());
            item.setGoodsId(cartDO.getGoodsId());
            item.setShopNumber(cartDO.getShopNumber());
            item.setQuantity(cartDO.getQuantity());
            item.setSelected(cartDO.getSelected());
            item.setCreateTime(cartDO.getCreateTime());

            String goodsCacheKey = "one-coupon_merchant-admin:goods:" + cartDO.getGoodsId();
            var goodsCache = stringRedisTemplate.opsForHash().entries(goodsCacheKey);
            if (!goodsCache.isEmpty()) {
                item.setGoodsName(getStr(goodsCache, "name"));
                item.setMainImage(getStr(goodsCache, "mainImage"));
                item.setPrice(getDecimal(goodsCache, "price"));
                item.setOriginalPrice(getDecimal(goodsCache, "originalPrice"));
                item.setGoodsStatus(getInt(goodsCache, "status"));
                item.setGoodsStock(getInt(goodsCache, "stock"));
            } else {
                item.setGoodsName("商品已下架");
                item.setPrice(BigDecimal.ZERO);
                item.setOriginalPrice(BigDecimal.ZERO);
                item.setGoodsStatus(0);
                item.setGoodsStock(0);
            }

            if (item.getPrice() != null) {
                item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(cartDO.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP));
            }
            items.add(item);
        }
        return items;
    }

    private CartSummaryRespDTO calculateSummary(List<CartItemRespDTO> items) {
        CartSummaryRespDTO summary = new CartSummaryRespDTO();
        summary.setItems(items);

        int totalCount = 0;
        int selectedCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        BigDecimal selectedOriginalAmount = BigDecimal.ZERO;

        for (CartItemRespDTO item : items) {
            if (item.getGoodsStatus() != null && item.getGoodsStatus() == 1) {
                totalCount += item.getQuantity();
                if (item.getSubtotal() != null) {
                    totalAmount = totalAmount.add(item.getSubtotal());
                }

                if (item.getSelected() != null && item.getSelected() == 1) {
                    selectedCount += item.getQuantity();
                    if (item.getSubtotal() != null) {
                        selectedAmount = selectedAmount.add(item.getSubtotal());
                    }
                    if (item.getOriginalPrice() != null && item.getPrice() != null) {
                        BigDecimal itemOriginal = item.getOriginalPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        selectedOriginalAmount = selectedOriginalAmount.add(itemOriginal);
                    }
                }
            }
        }

        BigDecimal savedAmount = selectedOriginalAmount.subtract(selectedAmount)
                .setScale(2, RoundingMode.HALF_UP);
        if (savedAmount.compareTo(BigDecimal.ZERO) < 0) {
            savedAmount = BigDecimal.ZERO;
        }

        BigDecimal taxAmount = selectedAmount.multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = selectedAmount.add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        summary.setTotalCount(totalCount);
        summary.setSelectedCount(selectedCount);
        summary.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        summary.setSelectedAmount(selectedAmount.setScale(2, RoundingMode.HALF_UP));
        summary.setSavedAmount(savedAmount);
        summary.setTaxAmount(taxAmount);
        summary.setPayableAmount(payableAmount);
        return summary;
    }

    private void deleteCartCache(Long userId) {
        String cacheKey = String.format(CART_CACHE_KEY, userId);
        stringRedisTemplate.delete(cacheKey);
    }

    private String getStr(java.util.Map<Object, Object> map, String key) {
        Object val = map.get(key);
        return val == null ? null : val.toString();
    }

    private BigDecimal getDecimal(java.util.Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().isEmpty()) return null;
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer getInt(java.util.Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().isEmpty()) return null;
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
