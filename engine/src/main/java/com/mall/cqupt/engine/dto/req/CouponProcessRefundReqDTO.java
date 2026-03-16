package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 处理优惠券结算单退款请求参数实体
 */
@Data
public class CouponProcessRefundReqDTO {

    /**
     * 订单ID
     */
    @Schema(description = "订单ID", required = true)
    private Long orderId;
}
