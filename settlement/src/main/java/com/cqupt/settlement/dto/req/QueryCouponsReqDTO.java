package com.cqupt.settlement.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 查询用户优惠券请求参数
 */
@Data
@Schema(description = "查询用户优惠券请求参数")
public class QueryCouponsReqDTO {

    @Schema(description = "用户ID", required = true)
    private Long userId;

    @Schema(description = "分页页码")
    private Integer pageNum;

    @Schema(description = "分页大小")
    private Integer pageSize;
}
