package com.mall.cqupt.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商品库存调整参数")
public class GoodsStockReqDTO {

    @Schema(description = "商品ID", required = true)
    private Long goodsId;

    @Schema(description = "调整数量（正数增加，负数减少）", required = true)
    private Integer quantity;
}
