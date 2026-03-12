package com.mall.cqupt.engine.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.engine.dao.entity.CouponTemplateRemindDO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindPageQueryReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindPageQueryRespDTO;

/**
 * 优惠券预约提醒业务逻辑层
 */
public interface CouponTemplateRemindService extends IService<CouponTemplateRemindDO> {

    /**
     * 创建抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    boolean createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam);

    /**
     * 分页查询抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    IPage<CouponTemplateRemindPageQueryRespDTO> pageQueryCouponRemind(CouponTemplateRemindPageQueryReqDTO requestParam);

    /**
     * 取消抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    boolean cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam);
}

