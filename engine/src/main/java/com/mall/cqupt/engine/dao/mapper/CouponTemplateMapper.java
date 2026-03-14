package com.mall.cqupt.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 优惠券模板数据库持久层
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {
    /**
     * 自减优惠券模板库存
     *
     * @param couponTemplateId 优惠券模板 ID
     * @return 是否发生记录变更
     */
    int decrementCouponTemplateStock(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") Long couponTemplateId, @Param("decrementStock") Long decrementStock);


    List<CouponTemplateDO> listByShopAndIds(@Param("queryList") List<CouponTemplateQueryDTO> queryList);
}
