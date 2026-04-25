package com.mall.cqupt.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.UserGoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsCategoryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsPageQueryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsCategoryService;
import com.mall.cqupt.merchant.admin.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "用户端商品浏览")
public class UserGoodsController {

    private final GoodsService goodsService;
    private final GoodsCategoryService goodsCategoryService;

    @Operation(summary = "用户端分页查询上架商品")
    @GetMapping("/api/user/goods/page")
    public Result<IPage<GoodsPageQueryRespDTO>> pageUserGoods(UserGoodsPageQueryReqDTO requestParam) {
        return Results.success(goodsService.pageQueryUserGoods(requestParam));
    }

    @Operation(summary = "用户端查询上架商品详情")
    @GetMapping("/api/user/goods/find")
    public Result<GoodsQueryRespDTO> findUserGoods(String goodsId, Long shopNumber) {
        return Results.success(goodsService.findUserGoodsById(goodsId, shopNumber));
    }

    @Operation(summary = "用户端查询启用商品分类树")
    @GetMapping("/api/user/goods-category/tree")
    public Result<List<GoodsCategoryRespDTO>> listUserCategoryTree() {
        return Results.success(goodsCategoryService.listCategoryTree());
    }
}
