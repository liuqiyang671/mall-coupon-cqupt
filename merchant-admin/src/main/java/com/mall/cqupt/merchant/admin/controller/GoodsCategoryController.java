package com.mall.cqupt.merchant.admin.controller;

import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.GoodsCategorySaveReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.GoodsCategoryRespDTO;
import com.mall.cqupt.merchant.admin.service.GoodsCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "商品分类管理")
public class GoodsCategoryController {

    private final GoodsCategoryService goodsCategoryService;

    @Operation(summary = "创建商品分类")
    @NoDuplicateSubmit(message = "请勿短时间内重复创建分类")
    @PostMapping("/api/merchant-admin/goods-category/save")
    public Result<Void> saveCategory(@RequestBody GoodsCategorySaveReqDTO requestParam) {
        goodsCategoryService.createCategory(requestParam);
        return Results.success();
    }

    @Operation(summary = "修改商品分类")
    @NoDuplicateSubmit(message = "请勿短时间内重复修改分类")
    @PostMapping("/api/merchant-admin/goods-category/update")
    public Result<Void> updateCategory(String categoryId, @RequestBody GoodsCategorySaveReqDTO requestParam) {
        goodsCategoryService.updateCategory(categoryId, requestParam);
        return Results.success();
    }

    @Operation(summary = "删除商品分类")
    @PostMapping("/api/merchant-admin/goods-category/delete")
    public Result<Void> deleteCategory(String categoryId) {
        goodsCategoryService.deleteCategory(categoryId);
        return Results.success();
    }

    @Operation(summary = "查询分类详情")
    @GetMapping("/api/merchant-admin/goods-category/find")
    public Result<GoodsCategoryRespDTO> findCategory(String categoryId) {
        return Results.success(goodsCategoryService.findCategoryById(categoryId));
    }

    @Operation(summary = "查询分类树")
    @GetMapping("/api/merchant-admin/goods-category/tree")
    public Result<List<GoodsCategoryRespDTO>> listCategoryTree() {
        return Results.success(goodsCategoryService.listCategoryTree());
    }

    @Operation(summary = "更新分类状态")
    @PostMapping("/api/merchant-admin/goods-category/update-status")
    public Result<Void> updateCategoryStatus(String categoryId, Integer status) {
        goodsCategoryService.updateCategoryStatus(categoryId, status);
        return Results.success();
    }
}
