package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mall.cqupt.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import com.mall.cqupt.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.mall.cqupt.merchant.admin.service.CouponTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.cqupt.merchant.admin.service.chain.MerchantAdminChainContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.mall.cqupt.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;

@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final MerchantAdminChainContext merchantAdminChainContext;

    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateMapper.insert(couponTemplateDO);
    }
}