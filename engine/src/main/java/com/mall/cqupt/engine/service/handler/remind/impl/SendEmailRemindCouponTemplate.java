package com.mall.cqupt.engine.service.handler.remind.impl;

import com.mall.cqupt.engine.service.handler.remind.RemindCouponTemplate;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;

/**
 * 发送邮件的方式提醒用户抢券
 */
public class SendEmailRemindCouponTemplate implements RemindCouponTemplate {
    @Override
    public boolean remind(RemindCouponTemplateDTO remindCouponTemplateDTO) {
        // 暂时空实现
        return true;
    }
}
