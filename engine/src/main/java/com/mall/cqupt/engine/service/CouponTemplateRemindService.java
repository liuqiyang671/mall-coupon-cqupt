package com.mall.cqupt.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.engine.dao.entity.CouponTemplateRemindDO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRemindCreateReqDTO;

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
}
