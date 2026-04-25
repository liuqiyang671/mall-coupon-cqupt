package com.mall.cqupt.engine.controller;

import com.mall.cqupt.engine.dto.req.CartAddReqDTO;
import com.mall.cqupt.engine.dto.req.CartSelectReqDTO;
import com.mall.cqupt.engine.dto.req.CartUpdateQuantityReqDTO;
import com.mall.cqupt.engine.dto.resp.CartSummaryRespDTO;
import com.mall.cqupt.engine.service.CartService;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "购物车管理")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "添加商品到购物车")
    @NoDuplicateSubmit(message = "请勿短时间内重复添加购物车")
    @PostMapping("/api/engine/cart/add")
    public Result<Void> addToCart(@RequestBody CartAddReqDTO requestParam) {
        cartService.addToCart(requestParam);
        return Results.success();
    }

    @Operation(summary = "修改购物车商品数量")
    @PostMapping("/api/engine/cart/update-quantity")
    public Result<Void> updateQuantity(@RequestBody CartUpdateQuantityReqDTO requestParam) {
        cartService.updateQuantity(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除购物车商品")
    @PostMapping("/api/engine/cart/remove")
    public Result<Void> removeItem(@RequestParam Long cartId) {
        cartService.removeItem(cartId);
        return Results.success();
    }

    @Operation(summary = "批量删除购物车商品")
    @PostMapping("/api/engine/cart/remove-batch")
    public Result<Void> removeItems(@RequestBody List<Long> cartIds) {
        cartService.removeItems(cartIds);
        return Results.success();
    }

    @Operation(summary = "清空购物车")
    @PostMapping("/api/engine/cart/clear")
    public Result<Void> clearCart() {
        cartService.clearCart();
        return Results.success();
    }

    @Operation(summary = "获取购物车汇总")
    @GetMapping("/api/engine/cart/summary")
    public Result<CartSummaryRespDTO> getCartSummary() {
        return Results.success(cartService.getCartSummary());
    }

    @Operation(summary = "更新购物车选中状态")
    @PostMapping("/api/engine/cart/select")
    public Result<Void> updateSelected(@RequestBody CartSelectReqDTO requestParam) {
        cartService.updateSelected(requestParam);
        return Results.success();
    }
}
