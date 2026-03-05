package com.mall.cqupt.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 新增商家优惠券模板
     *
     * @param requestParam 请求参数
     */
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);

    /**
     * 分页查询商家优惠券模板
     *
     * @param requestParam 请求参数
     * @return 商家优惠券模板分页数据
     */
    IPage<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);

    /**
     * 增加优惠券模板发行量
     *
     * @param requestParam 请求参数
     */
    void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam);
}

