package com.mall.cqupt.lqy.distribution.mq.event;


import com.mall.cqupt.lqy.distribution.remote.dto.resp.CouponTemplateQueryRemoteRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券模板任务执行事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateExecuteEvent {

    /**
     * 优惠券分发任务id
     */
    private String couponTaskId;

    /**
     * 优惠券模板
     */
    private CouponTemplateQueryRemoteRespDTO couponTemplate;

    /**
     * 用户id
     */
    private String userId;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
