package cn.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.mall.cqupt.merchant.admin.coomon.enums.CouponTemplateStatusEnum;
import cn.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import cn.mall.cqupt.merchant.admin.dao.mapper.CouponTemplateMapper;
import cn.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import cn.mall.cqupt.merchant.admin.service.CouponTemplateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    public void saveCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateMapper.insert(couponTemplateDO);
    }
}