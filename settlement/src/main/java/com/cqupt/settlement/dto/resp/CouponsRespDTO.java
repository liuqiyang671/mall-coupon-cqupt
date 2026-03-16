package com.cqupt.settlement.dto.resp;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询用户可/不可用券返回对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "查询用户可/不可用优惠券响应参数")
public class CouponsRespDTO {

    @Schema(description = "查询用户可用优惠券响应参数")
    private IPage<QueryCouponsRespDTO> availableCoupons;

    @Schema(description = "查询用户不可用优惠券响应参数")
    private IPage<QueryCouponsRespDTO> unavailableCoupons;
}
