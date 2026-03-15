package com.cqupt.settlement.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 折扣券数据库持久层实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountCouponDO extends CouponTemplateDO{

    /**
     * 折扣比例
     */
    private Double discountRate;
}
