package com.mall.cqupt.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mall.cqupt.engine.common.constant.EngineRedisConstant;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.enums.RedisStockDecrementErrorEnum;
import com.mall.cqupt.engine.dao.entity.CouponTemplateDO;
import com.mall.cqupt.engine.dao.entity.UserCouponDO;
import com.mall.cqupt.engine.dao.mapper.CouponTemplateMapper;
import com.mall.cqupt.engine.dao.mapper.UserCouponMapper;
import com.mall.cqupt.engine.dto.req.CouponTemplatePageQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateQueryReqDTO;
import com.mall.cqupt.engine.dto.req.CouponTemplateRedeemReqDTO;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.engine.mq.event.UserCouponDelayCloseEvent;
import com.mall.cqupt.engine.mq.producer.UserCouponDelayCloseProducer;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.engine.toolkit.StockDecrementReturnCombinedUtil;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券模板业务逻辑实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RBloomFilter<String> couponTemplateQueryBloomFilter;


    private final TransactionTemplate transactionTemplate;

    @Value("${one-coupon.user-coupon-list.save-cache.type:direct}")
    private String userCouponListSaveCacheType;

    private final static String STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH = "lua/stock_decrement_and_save_user_receive.lua";

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        // 查询 Redis 缓存中是否存在优惠券模板信息
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        Map<Object, Object> couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);

        // 如果存在直接返回，不存在需要通过布隆过滤器、缓存空值以及双重判定锁的形式读取数据库中的记录
        if (MapUtil.isEmpty(couponTemplateCacheMap)) {
            // 判断布隆过滤器是否存在指定模板 ID，不存在直接返回错误
            if(!couponTemplateQueryBloomFilter.contains(requestParam.getCouponTemplateId())){
                throw new ClientException("优惠券模板不存在");
            }

            // 查询 Redis 缓存中是否存在优惠券模板空值信息，如果有代表模板不存在，直接返回（直接讲key存入redis）
            String couponTemplateIsNullCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_IS_NULL_KEY, requestParam.getCouponTemplateId());
            Boolean hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateIsNullCacheKey);
            if(hasKeyFlag){
                throw new ClientException("优惠券模板不存在");
            }

            // 获取优惠券模板分布式锁
            RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId()));
            lock.lock();

            try {
                // 双重判定空值缓存是否存在，存在则继续抛异常
                hasKeyFlag = stringRedisTemplate.hasKey(couponTemplateIsNullCacheKey);
                if (hasKeyFlag) {
                    throw new ClientException("优惠券模板不存在");
                }

                // 通过双重判定锁优化大量请求无意义查询数据库
                couponTemplateCacheMap = stringRedisTemplate.opsForHash().entries(couponTemplateCacheKey);
                if (MapUtil.isEmpty(couponTemplateCacheMap)) {
                    LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getShopNumber, Long.parseLong(requestParam.getShopNumber()))
                            .eq(CouponTemplateDO::getId, Long.parseLong(requestParam.getCouponTemplateId()));
                    CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

                    // 优惠券模板不存在或者已过期加入空值缓存，并且抛出异常
                    if(couponTemplateDO == null) {
                        stringRedisTemplate.opsForValue().set(couponTemplateIsNullCacheKey, "",30, TimeUnit.MINUTES);
                        throw new ClientException("优惠券模板不存在或已过期");
                    }
                    // 通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
                    CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
                    Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
                    Map<String, String> actualCacheTargetMap = new HashMap<>();
                    for(Map.Entry<String, Object> entry: cacheTargetMap.entrySet()){
                        String key = entry.getKey();
                        String value = entry.getValue() != null ? entry.getValue().toString() : "";
                        actualCacheTargetMap.put(key, value);
                    }
                    // 将优惠卷模板加入缓存stream写法
//                    Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
//                            .collect(Collectors.toMap(
//                                    Map.Entry::getKey,
//                                    entry -> entry.getValue() != null ? entry.getValue().toString() : ""
//                            ));
                    // 定义 Lua 脚本：
                    //    第一句：使用 HMSET 设置 Hash 数据，unpack 用于将 ARGV 参数数组展开（从第1位到倒数第2位）
                    //    第二句：使用 EXPIREAT 设置该 Key 的过期时间戳（取 ARGV 数组的最后一位）
                    // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
                    String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                            "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";


                    List<String> keys = Collections.singletonList(couponTemplateCacheKey);
                    // 初始化 Lua 脚本所需的参数列表（ARGV），容量为：Map 键值对数量 * 2 + 1（存放过期时间）
                    List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
                    // 依次向参数列表中添加 Hash 的字段名（Field）和值（Value）
                    actualCacheTargetMap.forEach((key, value) -> {
                        args.add(key);
                        args.add(value);
                    });

                    // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
                    args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

                    // 执行 LUA 脚本
                    stringRedisTemplate.execute(
                            new DefaultRedisScript<>(luaScript, Long.class),
                            keys,
                            args.toArray()
                    );
                    couponTemplateCacheMap = cacheTargetMap.entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            } finally {
                lock.unlock();
            }
        }

        return BeanUtil.mapToBean(couponTemplateCacheMap, CouponTemplateQueryRespDTO.class, false, CopyOptions.create());
    }

    @Override
    public IPage<CouponTemplateQueryRespDTO> pageAvailableCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getStatus, 0)
                .ge(CouponTemplateDO::getValidEndTime, new Date())
                .like(StrUtil.isNotBlank(requestParam.getName()), CouponTemplateDO::getName, requestParam.getName())
                .eq(Objects.nonNull(requestParam.getSource()), CouponTemplateDO::getSource, requestParam.getSource())
                .eq(Objects.nonNull(requestParam.getTarget()), CouponTemplateDO::getTarget, requestParam.getTarget())
                .eq(Objects.nonNull(requestParam.getType()), CouponTemplateDO::getType, requestParam.getType())
                .orderByAsc(CouponTemplateDO::getValidStartTime)
                .orderByDesc(CouponTemplateDO::getCreateTime);
        if (StrUtil.isNotBlank(requestParam.getShopNumber())) {
            queryWrapper.eq(CouponTemplateDO::getShopNumber, parseLongParam(requestParam.getShopNumber(), "店铺编号格式不正确"));
        }
        if (StrUtil.isNotBlank(requestParam.getCouponTemplateId())) {
            queryWrapper.eq(CouponTemplateDO::getId, parseLongParam(requestParam.getCouponTemplateId(), "优惠券模板 ID 格式不正确"));
        }

        Page<CouponTemplateDO> page = new Page<>(requestParam.getCurrent(), requestParam.getSize());
        IPage<CouponTemplateDO> selectPage = couponTemplateMapper.selectPage(page, queryWrapper);
        return selectPage.convert(each -> {
            warmUpCouponTemplateCache(each);
            return BeanUtil.toBean(each, CouponTemplateQueryRespDTO.class);
        });
    }

    private void warmUpCouponTemplateCache(CouponTemplateDO couponTemplateDO) {
        if (couponTemplateDO == null || couponTemplateDO.getId() == null) {
            return;
        }
        String couponTemplateId = String.valueOf(couponTemplateDO.getId());
        couponTemplateQueryBloomFilter.add(couponTemplateId);
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateId);
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        stringRedisTemplate.opsForHash().putAll(couponTemplateCacheKey, actualCacheTargetMap);
    }

    private Long parseLongParam(String value, String message) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new ClientException(message);
        }
    }



    @Override
    public List<CouponTemplateDO> listCouponTemplateById(List<Long> couponTemplateIds, List<Long> shopNumbers) {
        //WHERE (
        //    (shop_number = 'S001' AND id = 10)
        //    OR
        //    (shop_number = 'S002' AND id = 20)
        //)
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .and(wrapper -> {
                    for (int i = 0; i < couponTemplateIds.size(); i++) {
                        int finalI = i; //Lambda 表达式内部如果想使用外部的局部变量，这个变量必须是 final 的，或者在逻辑上是“事实最终的（effectively final）”（即赋值后就不再改变）。
//                        原因： for 循环里的 i 每次迭代都在执行 i++，它的值一直在变。如果你直接在 Lambda 里写 shopNumbers.get(i)，编译器会直接报错。
//                        解法： 声明一个新的局部变量 int finalI = i;。对于每一次循环迭代，finalI 被赋值后就没有再被修改过，满足了 Lambda 的语法规范。
                        wrapper.or(innerWrapper -> innerWrapper.eq(CouponTemplateDO::getShopNumber, shopNumbers.get(finalI))
                                .eq(CouponTemplateDO::getId, couponTemplateIds.get(finalI)));
                    }
                });
        return couponTemplateMapper.selectList(queryWrapper);
    }
}
