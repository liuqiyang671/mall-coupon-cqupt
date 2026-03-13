package com.mall.cqupt.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 预约提醒方式枚举类
 */
@RequiredArgsConstructor
public enum CouponRemindTypeEnum {

    /**
     * 邮件提醒
     */
    EMAIL(0, "邮件提醒");

    @Getter
    private final int type;
    @Getter
    private final String describe;

    public static CouponRemindTypeEnum getByType(Integer type) {
        for(CouponRemindTypeEnum remindEnum : values()){
            if (remindEnum.getType() == type) {
                return remindEnum;
            }
        }
        return null;
    }

    public static String getDescribeByType(Integer type) {
        for(CouponRemindTypeEnum remindEnum : values()){
            if (remindEnum.getType() == type) {
                return remindEnum.getDescribe();
            }
        }
        return null;
    }
}
