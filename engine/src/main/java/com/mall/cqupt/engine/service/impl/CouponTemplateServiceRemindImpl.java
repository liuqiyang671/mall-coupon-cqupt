package com.mall.cqupt.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import com.mall.cqupt.engine.dao.entity.CouponTemplateRemindDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateRemindMapper;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateRemindService;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.engine.toolkit.CouponTemplateRemindUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券预约提醒业务逻辑实现层

 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceRemindImpl extends ServiceImpl<CouponTemplateRemindMapper, CouponTemplateRemindDO> implements CouponTemplateRemindService {

    private final CouponTemplateService couponTemplateService;
    private final CouponTemplateRemindMapper couponTemplateRemindMapper;

    @Override
    public boolean createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam) {
        return false;
    }

    @Override
    public List<CouponTemplateRemindQueryRespDTO> listCouponRemind(CouponTemplateRemindQueryReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateRemindDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateRemindDO.class)
                .eq(CouponTemplateRemindDO::getUserId, requestParam.getUserId());
        List<CouponTemplateRemindDO> couponTemplateRemindDOS = couponTemplateRemindMapper.selectList(queryWrapper);
        // 查出用户预约的信息
        if(couponTemplateRemindDOS == null || couponTemplateRemindDOS.isEmpty()){
            return new ArrayList<>();
        }
        // 根据优惠券id查询优惠券信息
        List<Long> couponIds = couponTemplateRemindDOS.stream().map(CouponTemplateRemindDO::getCouponTemplateId).toList();
        List<Long> shopNumbers = couponTemplateRemindDOS.stream().map(CouponTemplateRemindDO::getShopNumber).toList();
        List<CouponTemplateDO> couponTemplateDOS = couponTemplateService.listCouponTemplateById(couponIds, shopNumbers);
        List<CouponTemplateRemindQueryRespDTO> resp = BeanUtil.copyToList(couponTemplateDOS, CouponTemplateRemindQueryRespDTO.class);
        // 第一种实现：填充响应结果的其它信息
//        resp.forEach(each -> {
//            // 找到当前优惠券对应的预约提醒信息
//            couponTemplateRemindDOS.stream().filter(i -> i.getCouponTemplateId().equals(each.getId())).findFirst().ifPresent(i -> {
//                // 解析并填充预约提醒信息
//                CouponTemplateRemindUtil.fillRemindInformation(each, i.getInformation());
//            });
//        });
        // 第二种实现：
        // 1. 先将 List 转成 Map，Key 是优惠券 ID，Value 是提醒对象
        Map<Long, CouponTemplateRemindDO> remindMap = couponTemplateRemindDOS.stream()
                .collect(Collectors.toMap(CouponTemplateRemindDO::getCouponTemplateId, each -> each));
        // 2. 遍历结果集进行填充
        resp.forEach(each -> {
            // 直接从 Map 里取，时间复杂度从 O(m) 降到了 O(1)
            CouponTemplateRemindDO remindDO = remindMap.get(each.getId());
            if (remindDO != null) {
                CouponTemplateRemindUtil.fillRemindInformation(each, remindDO.getInformation());
            }
        });
        return resp;
    }

    public boolean cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam) {
        return false;
    }
}
