package com.mall.cqupt.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 优惠券模板查询接口请求参数实体
 */
@Data
@Schema(description = "优惠券预约抢券提醒请求参数实体")
public class CouponTemplateRemindCreateReqDTO {

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
     * 预约抢券时间点，可以接受开抢前五分钟到前一小时的预约，五分钟一个维度，以位图的形式，比如预约前十五分钟，就是1 << ((15 / 5) - 1)，也就是4(二进制100)
     */
    @Schema(description = "预约时间点", example = "4", required = true)
    private Long appointmentBitMap;

    /**
     * 提醒方式
     */
    @Schema(description = "提醒方式", example = "0", required = true)
    private Integer type;
}
