package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消抢券预约提醒接口请求参数实体
 */
@Data
@Schema(description = "取消优惠券预约抢券提醒参数实体")
public class CouponTemplateRemindCancelReqDTO {

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id", example = "1810966706881941507", required = true)
    private String couponTemplateId;

    /**
     * 用户id
     */
    @Schema(description = "用户id", example = "1810868149847928832", required = true)
    private String userId;

    /**
     * 提醒时间，比如五分钟，十分钟，十五分钟
     */
    @Schema(description = "提醒时间")
    private Integer remindTime;

    /**
     * 提醒方式
     */
    @Schema(description = "提醒方式", example = "0", required = true)
    private Integer type;

}
