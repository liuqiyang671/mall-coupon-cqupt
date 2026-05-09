package com.mall.cqupt.engine.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 领券中心优惠券模板分页查询请求参数实体
 */
@Data
@Schema(description = "领券中心优惠券模板分页查询请求参数实体")
public class CouponTemplatePageQueryReqDTO extends Page {

    /**
     * 店铺编号
     */
    @Schema(description = "店铺编号")
    private String shopNumber;

    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id")
    private String couponTemplateId;

    /**
     * 优惠券名称
     */
    @Schema(description = "优惠券名称")
    private String name;

    /**
     * 优惠券来源 0：店铺券 1：平台券
     */
    @Schema(description = "优惠券来源 0：店铺券 1：平台券")
    private Integer source;

    /**
     * 优惠对象 0：商品专属 1：全店通用
     */
    @Schema(description = "优惠对象 0：商品专属 1：全店通用")
    private Integer target;

    /**
     * 优惠类型 0：立减券 1：满减券 2：折扣券
     */
    @Schema(description = "优惠类型 0：立减券 1：满减券 2：折扣券")
    private Integer type;

    /**
     * 是否按预约提醒场景排序：可预约券优先，已开抢券靠后
     */
    @Schema(description = "是否按预约提醒场景排序")
    private Boolean remindFirst;
}
