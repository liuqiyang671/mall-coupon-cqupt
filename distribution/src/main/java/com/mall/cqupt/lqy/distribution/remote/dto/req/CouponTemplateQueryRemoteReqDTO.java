package com.mall.cqupt.lqy.distribution.remote.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券模板查询接口请求参数实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTemplateQueryRemoteReqDTO {

    /**
     * 店铺编号
     */
    private String shopNumber;

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;
}
