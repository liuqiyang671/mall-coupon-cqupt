package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "添加购物车请求参数")
public class CartAddReqDTO {

    @Schema(description = "商品ID", required = true)
    private Long goodsId;

    @Schema(description = "店铺编号", required = true)
    private Long shopNumber;

    @Schema(description = "商品数量", example = "1")
    private Integer quantity;
}
