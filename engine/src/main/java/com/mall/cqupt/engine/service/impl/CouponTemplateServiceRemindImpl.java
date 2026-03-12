package com.mall.cqupt.engine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.mall.cqupt.engine.dao.entity.CouponTemplateRemindDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateRemindMapper;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindPageQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindPageQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 优惠券预约提醒业务逻辑实现层

 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceRemindImpl extends ServiceImpl<CouponTemplateRemindMapper, CouponTemplateRemindDO> implements CouponTemplateRemindService {


    @Override
    public boolean createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam) {
        return false;
    }

    @Override
    public IPage<CouponTemplateRemindPageQueryRespDTO> pageQueryCouponRemind(CouponTemplateRemindPageQueryReqDTO requestParam) {
        return null;
    }

    public boolean cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam) {
        return false;
    }
}
