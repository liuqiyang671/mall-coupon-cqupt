package com.cqupt.settlement.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request parameters for applying a selected coupon to an order.
 */
@Data
public class ApplyCouponReqDTO {

    @Schema(description = "User ID", required = true)
    private Long userId;

    @Schema(description = "Shop number", required = true)
    private Long shopNumber;

    @Schema(description = "Order amount", required = true)
    private BigDecimal orderAmount;

    @Schema(description = "Order ID", required = true)
    private Long orderId;

    @Schema(description = "Order goods list")
    private List<QueryCouponGoodsReqDTO> goodsList;
}
