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

    private final UserCouponMapper userCouponMapper; // 数据库操作 Mapper
    private final StringRedisTemplate stringRedisTemplate; // Redis 操作客户端

    // 触发批量保存的水位线：每攒够 5000 条，执行一次数据库 Insert
    private final static int BATCH_USER_COUPON_SIZE = 5000;

    private final static String STOCK_DECREMENT_USER_RECORD_LUA_PATH = "lua/stock_decrement_user_record.lua";

    /**
     * 消费核心逻辑。
     * 注意：生产者每读一行 Excel 就会发一条消息过来。所以如果 Excel 有 1万行，这个方法会被调 1万次。
     */
    @Override
    public void onMessage(MessageWrapper<CouponTemplateExecuteEvent> messageWrapper) {
        // 打印日志：记录当前正在处理的消息。在高并发下可能日志量极大，建议线上环境调为 debug 级别或采样打印。
        log.info("[消费者] 优惠券分发到用户账号 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateExecuteEvent event = messageWrapper.getMessage();
        String couponTemplateId = event.getCouponTemplateId();

        // 核心削峰逻辑 - 水位线触发：
        // 只有当消息里携带的 "当前已发人数" 达到 5000 的倍数，或者携带了 "Excel读完" 的终结信号时，才真正去触碰数据库。
        // 也就是说，前 4999 条消息进到这个方法，连这个 if 都进不去，直接就执行结束了（相当于空跑，起到了过滤作用）。
        if (event.getBatchUserSetSize() >= BATCH_USER_COUPON_SIZE || event.getDistributionEndFlag()) {

            // 拼接 Redis 集合（Set）的 Key。之前执行 Lua 脚本时，成功的用户 ID 都被装进了这个 Set 里。
            String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());

            // 从 Redis Set 中弹出并删除数据。
            // 为什么是 BATCH_USER_COUPON_SIZE << 1 (即 5000 * 2 = 10000)？
            // 假设消费者在第 4999 条时宕机，重启后生产者可能已经把 Set 填到了 6000 条。
            // 如果这里严格只弹 5000 条，剩下的可能会因为错过了下一次“5000倍数”触发而被遗漏在 Redis 里。
            // 扩大一倍弹出量，保证只要触发了落库，就能把堆积的数据尽可能全带走。
            List<String> batchUserIds = stringRedisTemplate.opsForSet().pop(batchUserSetKey, BATCH_USER_COUPON_SIZE << 1);

            // 如果 Redis 里没数据（可能已经被其他并发的消费线程弹走了），直接返回，防止报错。
            if (CollUtil.isEmpty(batchUserIds)) {
                return;
            }

            // 直接传入 batchUserIds.size() 初始化 ArrayList 的容量。
            // 避免 ArrayList 底层因为数据量大而发生多次数组复制扩容，极致压榨性能。
            List<UserCouponDO> userCouponDOList = new ArrayList<>(batchUserIds.size());
            Date now = new Date(); // 统一获取一次时间，避免在循环里 new Date() 产生性能损耗

            // 遍历从 Redis 捞出来的真实用户 ID，组装成要插入数据库的实体对象 (DO)
            for (String each : batchUserIds) {
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .couponTemplateId(Long.parseLong(couponTemplateId))
                        .userId(Long.parseLong(each)) // 从 Redis 取出的用户 ID
                        .receiveTime(now)
                        .receiveCount(1) // 业务逻辑：代表第一次领取该优惠券
                        .validStartTime(now)
                        // 从消息体传递过来的 JSON 规则中解析出该优惠券的过期时间
                        .validEndTime(JSON.parseObject(event.getCouponTemplateConsumeRule()).getDate("validityPeriod"))
                        .source(CouponSourceEnum.PLATFORM.getType())
                        .status(CouponStatusEnum.EFFECTIVE.getType())
                        .createTime(now)
                        .updateTime(now)
                        .delFlag(0)
                        .build();
                userCouponDOList.add(userCouponDO);
            }

            // 调用专门的批量保存方法，这个方法内部做了严密的异常重试处理
            batchSaveUserCouponList(Long.parseLong(couponTemplateId), userCouponDOList);
        }
    }

    /**
     * 批量保存用户优惠券记录（带冲突自动排查机制）
     */
    private void batchSaveUserCouponList(Long couponTemplateId, List<UserCouponDO> userCouponDOList) {
        try {
            // 尝试将组装好的 5000 条数据一把推给 MySQL（MyBatis-Plus 的扩展批量插入）。
            // 如果一切顺利，这 5000 人就真的领到券了，方法结束。
            userCouponMapper.insert(userCouponDOList, userCouponDOList.size());
        } catch (Exception ex) {
            // 如果插入报错，捕获异常并探究根本原因。
            Throwable cause = ex.getCause();

            // BatchExecutorException 通常是因为这 5000 条数据里，有几条违反了数据库的“唯一索引（Unique Key）”。
            // 比如：(userId, couponTemplateId) 是唯一索引，意味着一个用户同一个模板只能有一张券。
            // 数据库遇到冲突会直接报错，导致这 5000 条全部插入失败（或者部分失败）。
            if (cause instanceof BatchExecutorException) {

                // 到底是谁已经领过券了？
                // 根据当前这 5000 人的 userId 列表，去数据库里查一查，看看谁的记录已经存在了。
                LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                        .eq(UserCouponDO::getCouponTemplateId, couponTemplateId)
                        .in(UserCouponDO::getUserId, userCouponDOList.stream().map(UserCouponDO::getUserId).toList());
                // 查询出这批人里，已经真正在数据库里有这张券的“捣蛋鬼”列表
                List<UserCouponDO> existentuserCouponDOList = userCouponMapper.selectList(queryWrapper);

                // 遍历这些已经存在的记录
                for (UserCouponDO each : existentuserCouponDOList) {
                    Long userId = each.getUserId();

                    // 使用 Iterator (迭代器) 遍历当前我们要插入的集合。
                    // 为什么不用 for-each 循环去 remove？因为在遍历集合时直接删除元素会抛出 ConcurrentModificationException 异常，必须用迭代器。
                    Iterator<UserCouponDO> iterator = userCouponDOList.iterator();
                    while (iterator.hasNext()) {
                        UserCouponDO item = iterator.next();
                        // 找到了那个导致插入报错的冲突数据！
                        if (item.getUserId().equals(userId)) {
                            // 从马上要重试的插入集合中，把这条数据安全地剔除出去
                            iterator.remove();
                            // TODO 应该添加到 t_coupon_task_fail 并标记错误原因（例如："重复领取"）
                        }
                    }
                }

                // 【递归重试】：雷排干净了，剩下的都是数据库里真的没有的人。
                // 只要集合没被删空，就自己调用自己，再次尝试批量插入。
                // 因为冲突数据已被剔除，下一次调用大概率会毫无阻碍地 insert 成功。
                if (CollUtil.isNotEmpty(userCouponDOList)) {
                    batchSaveUserCouponList(couponTemplateId, userCouponDOList);
                }
            }
        }
    }
}