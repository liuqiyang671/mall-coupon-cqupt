package com.cqupt.settlement.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 立减券（无门槛）数据库持久层实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoThresholdCouponDO extends CouponTemplateDO {

    /**
     * 优惠金额
     */
    private Integer discountAmount;
}
