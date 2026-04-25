package com.mall.cqupt.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "用户端商品分页查询参数")
public class UserGoodsPageQueryReqDTO extends Page {

    @Schema(description = "商品名称关键词")
    private String name;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "店铺编号")
    private Long shopNumber;

    @Schema(description = "最低价格")
    private BigDecimal minPrice;

    @Schema(description = "最高价格")
    private BigDecimal maxPrice;

    @Schema(description = "排序：recommend、priceAsc、priceDesc、salesDesc、newest")
    private String sort;
}
