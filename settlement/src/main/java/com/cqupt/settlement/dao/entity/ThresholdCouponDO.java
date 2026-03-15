package com.cqupt.settlement.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 满减券数据库持久层实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThresholdCouponDO extends CouponTemplateDO {

    /**
     * 满减门槛金额
     */
    private Integer thresholdAmount;

    /**
     * 优惠金额
     */
    private Integer discountAmount;
}
