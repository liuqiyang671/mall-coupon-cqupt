package com.mall.cqupt.engine.service;

import com.mall.cqupt.engine.dto.req.CartAddReqDTO;
import com.mall.cqupt.engine.dto.req.CartSelectReqDTO;
import com.mall.cqupt.engine.dto.req.CartUpdateQuantityReqDTO;
import com.mall.cqupt.engine.dto.resp.CartSummaryRespDTO;

public interface CartService {

    void addToCart(CartAddReqDTO requestParam);

    void updateQuantity(CartUpdateQuantityReqDTO requestParam);

    void removeItem(Long cartId);

    void removeItems(java.util.List<Long> cartIds);

    void clearCart();

    CartSummaryRespDTO getCartSummary();

    void updateSelected(CartSelectReqDTO requestParam);
}
