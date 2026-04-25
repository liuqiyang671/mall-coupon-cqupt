package com.mall.cqupt.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.GoodsPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.GoodsStockReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsPageQueryRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsQueryRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "商品管理")
public class GoodsController {

    private final GoodsService goodsService;

    @Operation(summary = "创建商品")
    @NoDuplicateSubmit(message = "请勿短时间内重复创建商品")
    @PostMapping("/api/merchant-admin/goods/save")
    public Result<Void> saveGoods(@RequestBody GoodsSaveReqDTO requestParam) {
        goodsService.createGoods(requestParam);
        return Results.success();
    }

    @Operation(summary = "修改商品")
    @NoDuplicateSubmit(message = "请勿短时间内重复修改商品")
    @PostMapping("/api/merchant-admin/goods/update")
    public Result<Void> updateGoods(String goodsId, @RequestBody GoodsSaveReqDTO requestParam) {
        goodsService.updateGoods(goodsId, requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询商品")
    @GetMapping("/api/merchant-admin/goods/page")
    public Result<IPage<GoodsPageQueryRespDTO>> pageQueryGoods(GoodsPageQueryReqDTO requestParam) {
        return Results.success(goodsService.pageQueryGoods(requestParam));
    }

    @Operation(summary = "查询商品详情")
    @GetMapping("/api/merchant-admin/goods/find")
    public Result<GoodsQueryRespDTO> findGoods(String goodsId) {
        return Results.success(goodsService.findGoodsById(goodsId));
    }

    @Operation(summary = "更新商品状态（上架/下架/违规下架）")
    @PostMapping("/api/merchant-admin/goods/update-status")
    public Result<Void> updateGoodsStatus(String goodsId, Integer status) {
        goodsService.updateGoodsStatus(goodsId, status);
        return Results.success();
    }

    @Operation(summary = "调整商品库存")
    @NoDuplicateSubmit(message = "请勿短时间内重复调整库存")
    @PostMapping("/api/merchant-admin/goods/adjust-stock")
    public Result<Void> adjustStock(@RequestBody GoodsStockReqDTO requestParam) {
        goodsService.adjustStock(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除商品")
    @PostMapping("/api/merchant-admin/goods/delete")
    public Result<Void> deleteGoods(String goodsId) {
        goodsService.deleteGoods(goodsId);
        return Results.success();
    }

    @Operation(summary = "批量查询商品信息")
    @PostMapping("/api/merchant-admin/goods/list-by-ids")
    public Result<List<GoodsQueryRespDTO>> listGoodsByIds(@RequestBody List<String> goodsIds) {
        return Results.success(goodsService.listGoodsByIds(goodsIds));
    }
}
