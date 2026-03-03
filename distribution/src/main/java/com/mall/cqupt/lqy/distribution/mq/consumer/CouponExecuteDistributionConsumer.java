package com.mall.cqupt.lqy.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Singleton;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.mall.cqupt.lqy.distribution.common.constant.DistributionRedisConstant;
import com.mall.cqupt.lqy.distribution.common.constant.DistributionRocketMQConstant;
import com.mall.cqupt.lqy.distribution.common.constant.EngineRedisConstant;
import com.mall.cqupt.lqy.distribution.common.enums.CouponSourceEnum;
import com.mall.cqupt.lqy.distribution.common.enums.CouponStatusEnum;
import com.mall.cqupt.lqy.distribution.dao.entity.UserCouponDO;
import com.mall.cqupt.lqy.distribution.dao.mapper.UserCouponMapper;
import com.mall.cqupt.lqy.distribution.mq.base.MessageWrapper;
import com.mall.cqupt.lqy.distribution.mq.event.CouponTemplateExecuteEvent;
import com.mall.cqupt.lqy.distribution.remote.dto.resp.CouponTemplateQueryRemoteRespDTO;
import com.mall.cqupt.lqy.distribution.toolkit.StockDecrementReturnCombinedUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchExecutorException;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 优惠券执行分发到用户消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_TOPIC_KEY,
        consumerGroup = DistributionRocketMQConstant.TEMPLATE_EXECUTE_DISTRIBUTION_CG_KEY
)
@Slf4j(topic = "CouponExecuteDistributionConsumer")
public class CouponExecuteDistributionConsumer implements RocketMQListener<MessageWrapper<CouponTemplateExecuteEvent>> {

    private final UserCouponMapper userCouponMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private final static int BATCH_USER_COUPON_SIZE = 5000;
    private final static String STOCK_DECREMENT_USER_RECORD_LUA_PATH = "lua/stock_decrement_user_record.lua";

    @Override
    public void onMessage(MessageWrapper<CouponTemplateExecuteEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 优惠券分发到用户账号 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateExecuteEvent event = messageWrapper.getMessage();
        String couponTemplateId = event.getCouponTemplateId();

        // 当保存用户优惠券集合达到批量保存数量或分发任务结束标识为 TRUE
        if (event.getBatchUserSetSize() >= BATCH_USER_COUPON_SIZE || event.getDistributionEndFlag()) {
            // 获取保存在 Redis 中的用户临时存储结果，弹出后数据即删除，如果 batchUserSetKey 中没有数据将会被自动删除
            // 为什么 BATCH_USER_COUPON_SIZE << 1 而不是直接用 BATCH_USER_COUPON_SIZE？同上面说的，考虑宕机结果。为什么不 +1 或者 +2，是因为宕机多少次不好说，不要相信极端情况，只能粗暴 *2
            String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());
            List<String> batchUserIds = stringRedisTemplate.opsForSet().pop(batchUserSetKey, BATCH_USER_COUPON_SIZE << 1);
            if (CollUtil.isEmpty(batchUserIds)) {
                return;
            }

            // 因为 batchUserIds 数据较多，ArrayList 会进行数次扩容，为了避免额外性能消耗，直接初始化 batchUserIds 大小的数组
            List<UserCouponDO> userCouponDOList = new ArrayList<>(batchUserIds.size());
            Date now = new Date();

            // 构建 userCouponDOList 用户优惠券批量数组
            for (String each : batchUserIds) {
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .couponTemplateId(Long.parseLong(couponTemplateId))
                        .userId(Long.parseLong(each))
                        .receiveTime(now)
                        .receiveCount(1) // 代表第一次领取该优惠券
                        .validStartTime(now)
                        .validEndTime(JSON.parseObject(event.getCouponTemplateConsumeRule()).getDate("validityPeriod"))
                        .source(CouponSourceEnum.PLATFORM.getType())
                        .status(CouponStatusEnum.EFFECTIVE.getType())
                        .createTime(new Date())
                        .updateTime(new Date())
                        .delFlag(0)
                        .build();
                userCouponDOList.add(userCouponDO);
            }

            // 平台优惠券每个用户限领一次。批量新增用户优惠券记录，底层通过递归方式直到全部新增成功
            batchSaveUserCouponList(Long.parseLong(couponTemplateId), userCouponDOList);
        }
    }

    private void batchSaveUserCouponList(Long couponTemplateId, List<UserCouponDO> userCouponDOList) {
        // MyBatis-Plus 批量执行用户优惠券记录
        try {
            userCouponMapper.insert(userCouponDOList, userCouponDOList.size());
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BatchExecutorException) {
                LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                        .eq(UserCouponDO::getCouponTemplateId, couponTemplateId)
                        .in(UserCouponDO::getUserId, userCouponDOList.stream().map(UserCouponDO::getUserId).toList());
                List<UserCouponDO> existentuserCouponDOList = userCouponMapper.selectList(queryWrapper);
                // 遍历已经存在的集合，获取 userId，并从需要新增的集合中移除匹配的元素
                for (UserCouponDO each : existentuserCouponDOList) {
                    Long userId = each.getUserId();

                    // 使用迭代器遍历需要新增的集合，安全移除元素
                    Iterator<UserCouponDO> iterator = userCouponDOList.iterator();
                    while (iterator.hasNext()) {
                        UserCouponDO item = iterator.next();
                        if (item.getUserId().equals(userId)) {
                            iterator.remove();
                            // TODO 应该添加到 t_coupon_task_fail 并标记错误原因
                        }
                    }
                }

                // 采用递归方式重试，直到不存在重复的记录为止
                if (CollUtil.isNotEmpty(userCouponDOList)) {
                    batchSaveUserCouponList(couponTemplateId, userCouponDOList);
                }
            }
        }
    }
}