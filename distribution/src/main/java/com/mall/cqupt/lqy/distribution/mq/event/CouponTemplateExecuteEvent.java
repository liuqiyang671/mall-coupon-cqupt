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
     * 通知方式，可组合使用 0：站内信 1：弹框推送 2：邮箱 3：短信
     */
    private String notifyType;

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;

    /**
     * 消耗规则
     */
    private String couponTemplateConsumeRule;

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

    /**
     * 批量保存用户优惠券 Set 长度，默认满 5000 才会批量保存数据库
     */
    private Long batchUserSetSize;

    /**
     * 分发结束标识
     */
    private Boolean distributionEndFlag;
}

