package com.mall.cqupt.lqy.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mall.cqupt.lqy.distribution.common.constant.DistributionRedisConstant;
import com.mall.cqupt.lqy.distribution.common.constant.DistributionRocketMQConstant;
import com.mall.cqupt.lqy.distribution.common.enums.CouponSourceEnum;
import com.mall.cqupt.lqy.distribution.common.enums.CouponStatusEnum;
import com.mall.cqupt.lqy.distribution.common.enums.CouponTaskStatusEnum;
import com.mall.cqupt.lqy.distribution.dao.entity.CouponTaskDO;
import com.mall.cqupt.lqy.distribution.dao.entity.CouponTemplateDO;
import com.mall.cqupt.lqy.distribution.dao.entity.UserCouponDO;
import com.mall.cqupt.lqy.distribution.dao.mapper.CouponTaskMapper;
import com.mall.cqupt.lqy.distribution.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.lqy.distribution.dao.mapper.UserCouponMapper;
import com.mall.cqupt.lqy.distribution.dao.sharding.DBShardingUtil;
import com.mall.cqupt.lqy.distribution.mq.base.MessageWrapper;
import com.mall.cqupt.lqy.distribution.mq.event.CouponTemplateExecuteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchExecutorException;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
    private final CouponTaskMapper couponTaskMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    // 触发批量保存的水位线：每攒够 5000 条，执行一次数据库 Insert
    private final static int BATCH_USER_COUPON_SIZE = 5000;

    private final static String STOCK_DECREMENT_USER_RECORD_LUA_PATH = "lua/stock_decrement_and_batch_save_user_record.lua";

    /**
     * 消费核心逻辑。
     * 注意：生产者每读一行 Excel 就会发一条消息过来。所以如果 Excel 有 1万行，这个方法会被调 1万次。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<CouponTemplateExecuteEvent> messageWrapper) {
        // 打印日志：记录当前正在处理的消息。在高并发下可能日志量极大，建议线上环境调为 debug 级别或采样打印。
        log.info("[消费者] 优惠券任务执行推送@分发到用户账号 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateExecuteEvent event = messageWrapper.getMessage();
        if(!event.getDistributionEndFlag() && event.getBatchUserSetSize() >= BATCH_USER_COUPON_SIZE){
            decrementCouponTemplateStockAndSaveUserCouponList(event);
        }
        // 分发任务结束标识为 TRUE，代表已经没有 Excel 记录了
        if(event.getDistributionEndFlag()){
            String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());
            // 查一下此时此刻，Redis里还剩下多少个等待真正发券的用户。 因为是尾部数据，可能不满 5000 条（比如还剩 3200 条）。
            Long batchUserIdsSize = stringRedisTemplate.opsForSet().size(batchUserSetKey);
            // 将查出来的真实剩余人数，塞进 event 对象中。
            event.setBatchUserSetSize(batchUserIdsSize);
            // 调用真正的业务方法去 MySQL 扣减实际库存，并将刚才那些用户落库（批量 Insert）。
            // 如果这 3200 人都有库存，这个方法会把他们全部发券，并从 Redis 里移走；
            // 如果 MySQL 里真实库存只剩 2000 了，这个方法只会给前 2000 人发券，而把剩下的 1200 人“遗弃”在 Redis 集合里！
            decrementCouponTemplateStockAndSaveUserCouponList(event);
            // 从 Redis 中把这个 Key 剩下的所有数据一次性全部弹出来（Integer.MAX_VALUE 代表能弹多少弹多少，直接清空）。
            List<String> batchUserIds = stringRedisTemplate.opsForSet().pop(batchUserSetKey, Integer.MAX_VALUE);
            // 此时待保存入库用户优惠券列表如果还有值，就意味着可能库存不足引起的
            if (CollUtil.isNotEmpty(batchUserIds)) {
                // TODO 应该添加到 t_coupon_task_fail 并标记错误原因
            }
            // 确保所有用户都已经接到优惠券后，设置优惠券推送任务完成时间
            CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                    .id(Long.parseLong(event.getCouponTaskId()))
                    .status(CouponTaskStatusEnum.SUCCESS.getStatus())
                    .completionTime(new Date())
                    .build();
            couponTaskMapper.updateById(couponTaskDO);
        }
    }

    private void decrementCouponTemplateStockAndSaveUserCouponList(CouponTemplateExecuteEvent event) {
        Long couponTemplateStock = decrementCouponTemplateStock(event, event.getBatchUserSetSize());
        // 如果等于 0 意味着已经没有了库存，直接返回即可
        if (couponTemplateStock <= 0L) {
            return;
        }

        // 获取 Redis 中待保存入库用户优惠券列表
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());
        List<String> batchUserIds = stringRedisTemplate.opsForSet().pop(batchUserSetKey, couponTemplateStock);

        // 直接传入 batchUserIds.size() 初始化 ArrayList 的容量。
        // 避免 ArrayList 底层因为数据量大而发生多次数组复制扩容，极致压榨性能。
        List<UserCouponDO> userCouponDOList = new ArrayList<>(batchUserIds.size());
        Date now = new Date();

        // 遍历从 Redis 捞出来的真实用户 ID，组装成要插入数据库的实体对象 (DO)
        for (String each : batchUserIds) {
            UserCouponDO userCouponDO = UserCouponDO.builder()
                    .couponTemplateId(Long.parseLong(event.getCouponTemplateId()))
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

        // 平台优惠券每个用户限领一次。批量新增用户优惠券记录，底层通过递归方式直到全部新增成功
        batchSaveUserCouponList(Long.parseLong(event.getCouponTemplateId()), userCouponDOList);
    }
    private Long decrementCouponTemplateStock(CouponTemplateExecuteEvent event, Long decrementStockSize) {
        // 通过乐观机制自减优惠券库存记录
        String couponTemplateId = event.getCouponTemplateId();
        int decremented = couponTemplateMapper.decrementCouponTemplateStock(event.getShopNumber(), Long.parseLong(couponTemplateId), decrementStockSize);

        // 如果修改记录失败，意味着优惠券库存已不足，需要重试获取到可自减的库存数值
        if(!SqlHelper.retBool(decremented)){
            LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                    .eq(CouponTemplateDO::getShopNumber, event.getShopNumber())
                    .eq(CouponTemplateDO::getId, Long.parseLong(couponTemplateId));
            CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);
            return decrementCouponTemplateStock(event, couponTemplateDO.getStock().longValue());
        }
        return decrementStockSize;
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

                // 查询已经存在的用户优惠券记录
                List<Long> userIds = userCouponDOList.stream().map(UserCouponDO::getUserId).toList();
                List<UserCouponDO> existingUserCoupons = getExistingUserCoupons(couponTemplateId, userIds);
                // 遍历已经存在的集合，获取 userId，并从需要新增的集合中移除匹配的元素
                for (UserCouponDO each : existingUserCoupons) {
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

    /**
     * 获取已经存在的用户优惠券集合
     * 为什么不直接使用 selectList 查询而是需要进行拆分再多次查询？因为一组用户 id 中可能会牵扯多个库，这样就会出现跨库查询问题
     * 为此我们按照不同用户 id 的数据库进行分类，比如一共有 5000 条记录，ds0 下有 2600 条记录，ds1 下有 2400 条记录，分别查询即可成功
     *
     * 先通过分库算法得到用户优惠券应该去哪一个库查，再进行查询
     * <p>
     * 如果直接使用以下语句查询会报某个数据库下某表不存在
     * LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
     * .eq(UserCouponDO::getCouponTemplateId, couponTemplateId)
     * .in(UserCouponDO::getUserId, userCouponDOList.stream().map(UserCouponDO::getUserId).toList());
     * List<UserCouponDO> existingUserCoupons = userCouponMapper.selectList(queryWrapper);
     *
     * @param couponTemplateId 优惠券模板 ID
     * @param userIds          用户 ID 集合
     * @return 已经存在的用户优惠券模板信息集合
     */
    public List<UserCouponDO> getExistingUserCoupons(Long couponTemplateId, List<Long> userIds) {
        // 1. 将 userIds 拆分到数据库中
        Map<Integer, List<Long>> databaseUserIdMap = splitUserIdsByDatabase(userIds);

        List<UserCouponDO> result = new ArrayList<>();
        // 2. 对每个数据库执行查询
        for (Map.Entry<Integer, List<Long>> entry : databaseUserIdMap.entrySet()) {
            List<Long> userIdSubset = entry.getValue();

            // 执行查询
            List<UserCouponDO> userCoupons = queryDatabase(couponTemplateId, userIdSubset);
            result.addAll(userCoupons);
        }

        return result;
    }

    private List<UserCouponDO> queryDatabase(Long couponTemplateId, List<Long> userIds) {
        LambdaQueryWrapper<UserCouponDO> queryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                .eq(UserCouponDO::getCouponTemplateId, couponTemplateId)
                .in(UserCouponDO::getUserId, userIds);

        return userCouponMapper.selectList(queryWrapper);
    }

    private Map<Integer, List<Long>> splitUserIdsByDatabase(List<Long> userIds) {
        Map<Integer, List<Long>> databaseUserIdMap = new HashMap<>();

        for (Long userId : userIds) {
            int databaseMod = DBShardingUtil.doUserCouponSharding(userId);
            databaseUserIdMap
                    .computeIfAbsent(databaseMod, k -> new ArrayList<>())
                    .add(userId);
        }

        return databaseUserIdMap;
    }
}