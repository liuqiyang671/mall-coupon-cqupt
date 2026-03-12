package com.mall.cqupt.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mall.cqupt.engine.common.constant.EngineRedisConstant;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.enums.RedisStockDecrementErrorEnum;
import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import com.mall.cqupt.engine.dao.entity.UserCouponDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.engine.dao.mapper.UserCouponMapper;
import com.mall.cqupt.engine.dto.req.CouponTemplateQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券模板业务逻辑实现层
 */
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private final TransactionTemplate transactionTemplate;

    private final static String STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH = "lua/stock_decrement_and_save_user_receive.lua";

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        // TODO 防止缓存穿透
        // 查询 Redis 缓存中是否存在优惠券模板信息
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        Map<Object, Object> couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);

        // 如果存在直接返回，不存在需要通过双重判定锁的形式读取数据库中的记录
        if (MapUtil.isEmpty(couponTemplateCacheMap)) {
            // 获取优惠券模板分布式锁
            RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId()));
            lock.lock();

            try {
                // 通过双重判定锁优化大量请求无意义查询数据库
                couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);
                if (MapUtil.isEmpty(couponTemplateCacheMap)) {
                    LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getShopNumber, Long.parseLong(requestParam.getShopNumber()))
                            .eq(CouponTemplateDO::getId, Long.parseLong(requestParam.getCouponTemplateId()));
                    CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

                    if (couponTemplateDO != null) {
                        // 通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
                        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
                        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
                        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                                ));
                        // 1. 先初始化一个目标类型的空 Map
                        //  Map<String, String> actualCacheTargetMap = new HashMap<>();
                        // 2. 遍历原 Map 的每一个 Entry（键值对）
                        // for (Map.Entry<String, Object> entry : cacheTargetMap.entrySet()) {
                        //    String key = entry.getKey();
                        //    Object value = entry.getValue();
                        //    // 3. 这里的逻辑与 Stream 中的 Lambda 一致：
                        //    // 如果值为 null，转换为空字符串；否则调用 toString()
                        //    String stringValue = (value != null) ? value.toString() : "";
                        //    // 4. 存入目标 Map
                        //    actualCacheTargetMap.put(key, stringValue);
                        //}
                        stringRedisTemplate.opsForHash().putAll(couponTemplateCacheKey, actualCacheTargetMap);
                        couponTemplateCacheMap = cacheTargetMap.entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        // couponTemplateCacheMap = new HashMap<>();
                        // 遍历 cacheTargetMap 的键值对
                        // for (Map.Entry<String, Object> entry : cacheTargetMap.entrySet()) {
                        //  将键和值放入新 Map 中
                        //  这里会自动将 String 类型的 Key 向上转型为 Object
                        //  couponTemplateCacheMap.put(entry.getKey(), entry.getValue());
                        //}
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return BeanUtil.mapToBean(couponTemplateCacheMap, CouponTemplateQueryRespDTO.class, false, CopyOptions.create());
    }

    @Override
    public void redeemCouponTemplate(CouponTemplateRedeemReqDTO requestParam) {
        // 验证缓存是否存在，保障数据存在并且缓存中存在
        CouponTemplateQueryRespDTO couponTemplate = findCouponTemplate(BeanUtil.toBean(requestParam, CouponTemplateQueryReqDTO.class));

        // 验证领取的优惠券是否在活动有效时间
        boolean isInTime = DateUtil.isIn(new Date(), couponTemplate.getValidStartTime(), couponTemplate.getValidEndTime());
        if (!isInTime) {
            // 一把来说优惠券领取时间不到的时候，前端不会放开调用请求，可以理解这是用户调用接口在“攻击”
            throw new ClientException("不满足优惠券领取时间");
        }

        // 获取 LUA 脚本，并保存到 Hutool 的单例管理容器，下次直接获取不需要加载
        DefaultRedisScript<Long> buildLuaScript = Singleton.get(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH)));
            redisScript.setResultType(Long.class);
            return redisScript;
        });

        // 验证用户是否符合优惠券领取条件
        JSONObject receiveRule = JSON.parseObject(couponTemplate.getReceiveRule());
        String limitPerPerson = receiveRule.getString("limitPerPerson");

        // 执行 LUA 脚本进行扣减库存以及增加 Redis 用户领券记录次数
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        String userCouponTemplateLimitCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIMIT_KEY, UserContext.getUserId(), requestParam.getCouponTemplateId());
        Long stockDecrementLuaResult = stringRedisTemplate.execute(
                buildLuaScript,
                ListUtil.of(couponTemplateCacheKey, userCouponTemplateLimitCacheKey),
                String.valueOf(couponTemplate.getValidEndTime().getTime()), limitPerPerson
        );

        // 判断 LUA 脚本执行返回类，如果失败根据类型返回报错提示
        long firstField = StockDecrementReturnCombinedUtil.extractFirstField(stockDecrementLuaResult);
        if (RedisStockDecrementErrorEnum.isFail(firstField)) {
            throw new ServiceException(RedisStockDecrementErrorEnum.fromType(firstField));
        }

        long extractSecondField = StockDecrementReturnCombinedUtil.extractSecondField(stockDecrementLuaResult);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                int decremented = couponTemplateMapper.decrementCouponTemplateStock(Long.parseLong(requestParam.getShopNumber()), Long.parseLong(requestParam.getCouponTemplateId()), 1L);
                if (!SqlHelper.retBool(decremented)) {
                    throw new ServiceException("优惠券已被领取完啦");
                }

                // 添加 Redis 用户领取的优惠券记录列表
                Date now = new Date();
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .couponTemplateId(Long.parseLong(requestParam.getCouponTemplateId()))
                        .userId(Long.parseLong(UserContext.getUserId()))
                        .source(requestParam.getSource())
                        .receiveCount(Long.valueOf(extractSecondField).intValue())
                        .status(0)
                        .receiveTime(now)
                        .validStartTime(now)
                        .validEndTime(DateUtil.offsetHour(now, JSON.parseObject(couponTemplate.getConsumeRule()).getInteger("validityPeriod")))
                        .build();
                userCouponMapper.insert(userCouponDO);

                // TODO 添加用户领取优惠券模板缓存记录
            } catch (Exception ex) {
                status.setRollbackOnly();
                // 自减用户领取的优惠券记录 xxx_优惠券ID_用户ID Value 是领取次数
                if (ex instanceof ServiceException) {
                    throw (ServiceException) ex;
                }
                throw new ServiceException("优惠券领取异常，请稍候再试");
            }
        });
    }
}
