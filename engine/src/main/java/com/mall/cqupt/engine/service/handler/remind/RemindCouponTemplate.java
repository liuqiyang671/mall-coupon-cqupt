package com.mall.cqupt.engine.service.handler.remind;

import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;

/**
 * 优惠券抢券提醒接口
 * <p>
 * 作者：优雅
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-18
 */
public interface RemindCouponTemplate {
    /**
     * 提醒用户抢券
     *
     * @param remindCouponTemplateDTO 提醒所需要的信息
     */
    boolean remind(RemindCouponTemplateDTO remindCouponTemplateDTO);

}
