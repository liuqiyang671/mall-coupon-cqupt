package com.cqupt.settlement.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 查询用户优惠券响应参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "查询用户优惠券响应参数")
public class QueryCouponsRespDTO {

    @Schema(description = "可用优惠券列表")
    private List<QueryCouponsDetailRespDTO> availableCoupons;

    @Schema(description = "不可用优惠券列表")
    private List<QueryCouponsDetailRespDTO> notAvailableCoupons;
}
