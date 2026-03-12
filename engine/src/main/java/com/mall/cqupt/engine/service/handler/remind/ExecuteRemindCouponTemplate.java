package com.mall.cqupt.engine.service.handler.remind;



import com.mall.cqupt.engine.common.enums.CouponRemindTypeEnum;
import com.mall.cqupt.engine.service.handler.remind.dto.RemindCouponTemplateDTO;
import com.mall.cqupt.engine.service.handler.remind.impl.SendEmailRemindCouponTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;


/**
 * 执行相应的抢券提醒
 */
@Component
@AllArgsConstructor
public class ExecuteRemindCouponTemplate {

    private final SendEmailRemindCouponTemplate sendEmailRemindCouponTemplate;

    /**
     * 执行提醒
     *
     * @param remindCouponTemplateDTO 需要的信息
     */
    public void executeRemindCouponTemplate(RemindCouponTemplateDTO remindCouponTemplateDTO) {
        switch (Objects.requireNonNull(CouponRemindTypeEnum.getByType(remindCouponTemplateDTO.getType()))) {
            case EMAIL:
                sendEmailRemindCouponTemplate.remind(remindCouponTemplateDTO);
                break;
            default:
                break;
        }

    }

}
