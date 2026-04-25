package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改购物车商品数量请求参数")
public class CartUpdateQuantityReqDTO {

    @Schema(description = "购物车项ID", required = true)
    private Long cartId;

    @Schema(description = "新数量", required = true)
    private Integer quantity;
}
