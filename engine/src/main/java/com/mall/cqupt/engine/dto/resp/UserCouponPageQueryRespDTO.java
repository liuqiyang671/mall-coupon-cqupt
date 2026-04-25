package com.mall.cqupt.engine.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 用户券包分页查询返回实体
 */
@Data
@Schema(description = "用户券包分页查询返回实体")
public class UserCouponPageQueryRespDTO {

    @Schema(description = "用户优惠券 ID")
    private Long id;

    @Schema(description = "优惠券模板 ID")
    private Long couponTemplateId;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "领取次数")
    private Integer receiveCount;

    @Schema(description = "券来源 0：领券中心 1：平台发放 2：店铺领取")
    private Integer source;

    @Schema(description = "用户优惠券状态 0：未使用 1：锁定 2：已使用 3：已过期 4：已撤回")
    private Integer status;

    @Schema(description = "领取时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date receiveTime;

    @Schema(description = "有效期开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validStartTime;

    @Schema(description = "有效期结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date validEndTime;

    @Schema(description = "使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date useTime;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "店铺编号")
    private String shopNumber;

    @Schema(description = "优惠对象 0：商品专属 1：全店通用")
    private Integer target;

    @Schema(description = "优惠商品编码")
    private String goods;

    @Schema(description = "优惠类型 0：立减券 1：满减券 2：折扣券")
    private Integer type;

    @Schema(description = "领取规则")
    private String receiveRule;

    @Schema(description = "消耗规则")
    private String consumeRule;
}
