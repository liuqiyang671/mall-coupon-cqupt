package com.mall.cqupt.engine.service.handler.remind.impl;

import com.mall.cqupt.engine.service.handler.remind.RemindCouponTemplate;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import org.springframework.stereotype.Component;

/**
 * 发送邮件的方式提醒用户抢券
 */

@Component
public class SendEmailRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 以邮件方式提醒用户抢券
     * @param remindCouponTemplateDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(RemindCouponTemplateDTO remindCouponTemplateDTO) {
        // 暂时空实现
        return true;
    }

}