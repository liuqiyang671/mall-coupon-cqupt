package com.mall.cqupt.engine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import com.mall.cqupt.engine.dto.req.CouponTemplatePageQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;

import java.util.List;


/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 创建商家优惠券模板
     *
     * @param requestParam 请求参数
     */
    CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam);

    /**
     * 分页查询领券中心可领取优惠券模板
     *
     * @param requestParam 请求参数
     * @return 可领取优惠券模板分页数据
     */
    IPage<CouponTemplateQueryRespDTO> pageAvailableCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);

    /**
     * 根据优惠券id集合查询出券信息
     *
     * @param couponTemplateIds 优惠券id集合
     */
    List<CouponTemplateDO> listCouponTemplateById(List<Long> couponTemplateIds, List<Long> shopNumbers);
}
