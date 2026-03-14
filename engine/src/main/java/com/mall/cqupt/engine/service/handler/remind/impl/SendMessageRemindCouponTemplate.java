package com.mall.cqupt.engine.service.handler.remind.impl;



import com.mall.cqupt.engine.service.handler.remind.RemindCouponTemplate;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import org.springframework.stereotype.Component;

/**
 * 短信方式提醒用户抢券
 */
@Component
public class SendMessageRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 以短信方式提醒用户抢券
     * @param remindCouponTemplateDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(RemindCouponTemplateDTO remindCouponTemplateDTO) {
        // 暂时空实现
        return true;
    }

}
