package com.cqupt.settlement.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 查询用户优惠券响应参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "查询用户优惠券响应参数")
public class QueryCouponsRespDTO {

    @Schema(description = "优惠券模板ID")
    private Long couponTemplateId;

    @Schema(description = "优惠券名称")
    private String couponName;

    @Schema(description = "领取时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;

    @Schema(description = "有效期开始时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validStartTime;

    @Schema(description = "有效期结束时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validEndTime;

    @Schema(description = "状态 0：未使用 1：锁定 2：已使用 3：已过期 4：已撤回")
    private Integer status;

}
