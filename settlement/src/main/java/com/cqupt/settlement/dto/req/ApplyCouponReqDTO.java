package com.cqupt.settlement.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 应用优惠券请求参数
 */
@Data
public class ApplyCouponReqDTO {

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "店铺编号", required = true)
    private Long shopNumber;

    @Schema(description = "订单金额", required = true)
    private BigDecimal orderAmount;

    @Schema(description = "订单ID", required = true)
    private Long orderId;
}
