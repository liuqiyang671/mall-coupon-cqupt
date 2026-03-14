package com.mall.cqupt.engine.service.handler.remind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送抢券提醒实体
 */
@Data
@Schema(description = "发送抢券提醒实体")
public class RemindCouponTemplateDTO {

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id", example = "1810966706881941507", required = true)
    private String couponTemplateId;

    /**
     * 优惠券名称
     */
    @Schema(description = "优惠券名称")
    private String name;

    /**
     * 店铺编号
     */
    @Schema(description = "店铺编号", example = "1810714735922956666", required = true)
    private String shopNumber;

    /**
     * 用户id
     */
    @Schema(description = "用户id", example = "1810868149847928832", required = true)
    private String userId;

    /**
     * 用户联系方式，可能是邮箱、手机号、等等
     */
    @Schema(description = "用户联系方式")
    private String contact;

    /**
     * 提醒方式
     */
    @Schema(description = "提醒方式", example = "0", required = true)
    private Integer type;

}
