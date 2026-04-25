package com.mall.cqupt.merchant.admin.controller;

import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.GoodsAttributeSaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsAttributeRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "商品属性管理")
public class GoodsAttributeController {

    private final GoodsAttributeService goodsAttributeService;

    @Operation(summary = "创建商品属性")
    @NoDuplicateSubmit(message = "请勿短时间内重复创建属性")
    @PostMapping("/api/merchant-admin/goods-attribute/save")
    public Result<Void> saveAttribute(@RequestBody GoodsAttributeSaveReqDTO requestParam) {
        goodsAttributeService.createAttribute(requestParam);
        return Results.success();
    }

    @Operation(summary = "修改商品属性")
    @NoDuplicateSubmit(message = "请勿短时间内重复修改属性")
    @PostMapping("/api/merchant-admin/goods-attribute/update")
    public Result<Void> updateAttribute(String attributeId, @RequestBody GoodsAttributeSaveReqDTO requestParam) {
        goodsAttributeService.updateAttribute(attributeId, requestParam);
        return Results.success();
    }

    @Operation(summary = "删除商品属性")
    @PostMapping("/api/merchant-admin/goods-attribute/delete")
    public Result<Void> deleteAttribute(String attributeId) {
        goodsAttributeService.deleteAttribute(attributeId);
        return Results.success();
    }

    @Operation(summary = "查询所有商品属性")
    @GetMapping("/api/merchant-admin/goods-attribute/list")
    public Result<List<GoodsAttributeRespDTO>> listAttributes() {
        return Results.success(goodsAttributeService.listAllAttributes());
    }
}
