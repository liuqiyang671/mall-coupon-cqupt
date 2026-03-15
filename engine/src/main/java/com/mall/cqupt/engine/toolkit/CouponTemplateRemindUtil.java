package com.mall.cqupt.engine.toolkit;

import cn.hutool.core.date.DateUtil;
import com.mall.cqupt.engine.common.enums.CouponRemindTypeEnum;
import com.mall.cqupt.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import com.mall.cqupt.framework.exception.ClientException;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 优惠券预约提醒工具类
 */
public class CouponTemplateRemindUtil {

    /**
     * 下一个类型的位移量，每个类型占用12个bit位，共计60分钟
     */
    private static final int NEXT_TYPE_BITS = 12;

    /**
     * 5分钟为一个间隔
     */
    private static final int TIME_INTERVAL = 5;

    /**
     * 提醒方式的数量
     */
    private static final int TYPE_COUNT = CouponRemindTypeEnum.values().length;

    /**
     * 填充预约信息
     * 逻辑：通过位运算解析 information 字段，还原出用户在哪些时间点、选择了哪些提醒方式
     */
    public static void fillRemindInformation(CouponTemplateRemindQueryRespDTO resp, Long information) {
        List<Date> dateList = new ArrayList<>();     // 存储解析出的提醒时间节点
        List<String> remindType = new ArrayList<>(); // 存储解析出的提醒方式描述
        Date validStartTime = resp.getValidStartTime(); // 优惠券开始领取的基准时间

        // 遍历所有可能的时间偏移节点（例如：15分钟前、30分钟前...）
        for (int i = 0; i < NEXT_TYPE_BITS; i++) {
            // 按时间节点倒叙遍历，即离开抢时间最久，离现在最近
            // 遍历每个时间节点下的提醒类型（例如：0-短信，1-站内信，2-App推送）
            for (int j = 0; j < TYPE_COUNT; j++) {

                /*
                 * 位运算核心逻辑：
                 * 1. (j * NEXT_TYPE_BITS + i)：计算当前组合在 Long 型字段中的偏移位数（Index）。
                 * 2. information >> offset：将目标位右移到最低位。
                 * 3. & 1：检查最低位是否为 1。如果是 1，说明用户勾选了该项。
                 */
                if (((information >> (j * NEXT_TYPE_BITS + i)) & 1) == 1) {

                    // 计算实际提醒时间：基准时间 - (偏移周期个数 * 每个周期的时间间隔)
                    // 例如：i=0, INTERVAL=15 -> 提前15分钟；i=1 -> 提前30分钟
                    Date date = DateUtil.offsetMinute(validStartTime, -((i + 1) * TIME_INTERVAL));
                    dateList.add(date);

                    // 根据类型索引 j 获取对应的枚举描述（如 "短信"、"系统通知"）
                    remindType.add(CouponRemindTypeEnum.getDescribeByType(j));
                }
            }
        }
        resp.setRemindTime(dateList);
        resp.setRemindType(remindType);
    }

    /**
     * 根据预约时间和预约类型计算bitmap
     */
    public static Long calculateBitMap(Integer remindTime, Integer type) {
        if (remindTime > TIME_INTERVAL * NEXT_TYPE_BITS)
            throw new ClientException("预约时间不能大于" + TIME_INTERVAL * NEXT_TYPE_BITS + "分钟");
        return 1L << (type * NEXT_TYPE_BITS + Math.max(0, remindTime / TIME_INTERVAL - 1));
    }
}
