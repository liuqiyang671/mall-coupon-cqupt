package com.cqupt.settlement.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 使用优惠券响应实体
 */
@Data
@Builder
public class ApplyCouponRespDTO {

    @Schema(description = "订单ID", required = true)
    private Long orderId;

    @Schema(description = "订单原始金额", required = true)
    private BigDecimal originalAmount;

    @Schema(description = "订单折后金额", required = true)
    private BigDecimal finalAmount;

    @Schema(description = "待应用的优惠券ID", required = true)
    private Long appliedCouponId;
}
