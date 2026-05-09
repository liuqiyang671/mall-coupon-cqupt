package com.cqupt.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.cqupt.settlement.common.context.UserContext;
import com.cqupt.settlement.dao.entity.UserCouponDO;
import com.cqupt.settlement.dao.mapper.UserCouponMapper;
import com.cqupt.settlement.dto.req.QueryCouponGoodsReqDTO;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.CouponsRespDTO;
import com.cqupt.settlement.dto.resp.CouponTemplateQueryRespDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsDetailRespDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;
import com.cqupt.settlement.service.CouponQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.config.RedisDistributedProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.cqupt.settlement.common.constant.SettlementRedisConstant.COUPON_TEMPLATE_KEY;
import static com.cqupt.settlement.common.constant.SettlementRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY;

/**
 * 查询用户可用 / 不可用优惠券列表接口
 */
@Service
@RequiredArgsConstructor
public class CouponQueryServiceImpl implements CouponQueryService {

    private final RedisDistributedProperties redisDistributedProperties;
    private final StringRedisTemplate stringRedisTemplate;

    // 当前应用基本上没有 CPU 操作，我们可以把这个线程池设置的稍微大一点
    // CPU核心数 / (1 - 阻塞系数)，阻塞系数看 CPU 处理性能，这个阻塞系数一般为0.8~0.9之间，可以取 0.8 或者 0.9。通过这种形式可以最大限度发挥出服务器 CPU 全部性能
    private final ExecutorService executorService = new ThreadPoolExecutor(
            calculateCorePoolSize(),
            calculateCorePoolSize() + (calculateCorePoolSize() >> 1),
            9999,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private Integer calculateCorePoolSize() {
        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        return new BigDecimal(cpuCoreNum).divide(new BigDecimal("0.2")).intValue();
    }

    @Override
    public QueryCouponsRespDTO listQueryUserCoupons(QueryCouponsReqDTO requestParam) {
        String userId = getQueryUserId(requestParam);
        // Step 1: 获取 Redis 中的用户优惠券列表
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(
                String.format(USER_COUPON_TEMPLATE_LIST_KEY, userId), 0, -1);

        if (rangeUserCoupons == null || rangeUserCoupons.isEmpty()) {
            return QueryCouponsRespDTO.builder()
                    .availableCoupons(new ArrayList<>())
                    .notAvailableCoupons(new ArrayList<>())
                    .build();
        }

        // 构建 Redis Key 列表
        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(this::buildCouponTemplateCacheKey)
                .toList();

        // 同步获取 Redis 数据并进行解析、转换和分区
        List<Object> rawCouponDataList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        // 解析 Redis 数据，并按 `goods` 字段进行分区处理
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = JSON.parseArray(JSON.toJSONString(rawCouponDataList), CouponTemplateQueryRespDTO.class)
                .stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCoupons = Collections.synchronizedList(new ArrayList<>());
        List<QueryCouponsDetailRespDTO> notAvailableCoupons = Collections.synchronizedList(new ArrayList<>());

        // Step 2: 并行处理 goodsEmptyList 和 goodsNotEmptyList
        CompletableFuture<Void> emptyGoodsTask = CompletableFuture.runAsync(() -> {
            processEmptyGoodsCoupons(goodsEmptyList, requestParam, availableCoupons, notAvailableCoupons);
        }, executorService);

        CompletableFuture<Void> notEmptyGoodsTask = CompletableFuture.runAsync(() -> {
            Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = requestParam.getGoodsList().stream()
                    .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity()));
            processNonEmptyGoodsCoupons(goodsNotEmptyList, goodsRequestMap, availableCoupons, notAvailableCoupons);
        }, executorService);

        // Step 3: 等待两个异步任务完成
        CompletableFuture.allOf(emptyGoodsTask, notEmptyGoodsTask).join();

        // 与业内标准一致，按最终优惠力度从大到小排序
        availableCoupons.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));

        // 构建最终结果并返回
        return QueryCouponsRespDTO.builder()
                .availableCoupons(availableCoupons)
                .notAvailableCoupons(notAvailableCoupons)
                .build();
    }

    // 处理空商品列表的优惠券逻辑
    private void processEmptyGoodsCoupons(List<CouponTemplateQueryRespDTO> goodsEmptyList, QueryCouponsReqDTO requestParam,
                                          List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        goodsEmptyList.forEach(each -> {
            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            handleCouponLogic(resultCouponDetail, jsonObject, requestParam.getOrderAmount(), availableCoupons, notAvailableCoupons);
        });
    }

    // 处理非空商品列表的优惠券逻辑
    private void processNonEmptyGoodsCoupons(List<CouponTemplateQueryRespDTO> goodsNotEmptyList, Map<String, QueryCouponGoodsReqDTO> goodsRequestMap,
                                             List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        goodsNotEmptyList.forEach(each -> {
            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
            if (couponGoods == null) {
                notAvailableCoupons.add(resultCouponDetail);
            } else {
                JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
                handleCouponLogic(resultCouponDetail, jsonObject, couponGoods.getGoodsAmount(), availableCoupons, notAvailableCoupons);
            }
        });
    }

    // 优惠券判断逻辑，根据条件判断放入可用或不可用列表
    private void handleCouponLogic(QueryCouponsDetailRespDTO resultCouponDetail, JSONObject jsonObject, BigDecimal amount,
                                   List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        BigDecimal termsOfUse = jsonObject.getBigDecimal("termsOfUse");
        BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");

        switch (resultCouponDetail.getType()) {
            case 0: // 立减券
                resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                availableCoupons.add(resultCouponDetail);
                break;
            case 1: // 满减券
                if (amount.compareTo(termsOfUse) >= 0) {
                    resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCoupons.add(resultCouponDetail);
                } else {
                    notAvailableCoupons.add(resultCouponDetail);
                }
                break;
            case 2: // 折扣券
                if (amount.compareTo(termsOfUse) >= 0) {
                    BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                    BigDecimal multiply = amount.multiply(discountRate);
                    if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                        resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    } else {
                        resultCouponDetail.setCouponAmount(multiply);
                    }
                    availableCoupons.add(resultCouponDetail);
                } else {
                    notAvailableCoupons.add(resultCouponDetail);
                }
                break;
            default:
                throw new ClientException("无效的优惠券类型");
        }
    }

    /**
     * 单线程版本，好理解一些。上面的多线程就是基于这个版本演进的
     */
    public QueryCouponsRespDTO listQueryUserCouponsBySync(QueryCouponsReqDTO requestParam) {
        String userId = getQueryUserId(requestParam);
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(String.format(USER_COUPON_TEMPLATE_LIST_KEY, userId), 0, -1);
        if (rangeUserCoupons == null || rangeUserCoupons.isEmpty()) {
            return QueryCouponsRespDTO.builder()
                    .availableCoupons(new ArrayList<>())
                    .notAvailableCoupons(new ArrayList<>())
                    .build();
        }

        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(this::buildCouponTemplateCacheKey)
                .toList();
        List<Object> couponTemplateList = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        List<CouponTemplateQueryRespDTO> couponTemplateDTOList = JSON.parseArray(JSON.toJSONString(couponTemplateList), CouponTemplateQueryRespDTO.class);
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = couponTemplateDTOList.stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCoupons = new ArrayList<>();
        List<QueryCouponsDetailRespDTO> notAvailableCoupons = new ArrayList<>();

        goodsEmptyList.forEach(each -> {
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCoupons.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal multiply = requestParam.getOrderAmount().multiply(jsonObject.getBigDecimal("discountRate"));
                        if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                            resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        } else {
                            resultQueryCouponDetail.setCouponAmount(multiply);
                        }
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = requestParam.getGoodsList().stream()
                .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity(), (existing, replacement) -> existing));

        goodsNotEmptyList.forEach(each -> {
            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
            if (couponGoods == null) {
                notAvailableCoupons.add(BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class));
                return;
            }
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                    availableCoupons.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                        resultQueryCouponDetail.setCouponAmount(couponGoods.getGoodsAmount().multiply(discountRate));
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        // 与业内标准一致，按最终优惠力度从大到小排序
        availableCoupons.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));

        return QueryCouponsRespDTO.builder()
                .availableCoupons(availableCoupons)
                .notAvailableCoupons(notAvailableCoupons)
                .build();
    }

    private String buildCouponTemplateCacheKey(String couponTemplateId) {
        return Optional.ofNullable(redisDistributedProperties.getPrefix()).orElse("") + String.format(COUPON_TEMPLATE_KEY, couponTemplateId);
    }

    private String getQueryUserId(QueryCouponsReqDTO requestParam) {
        if (requestParam.getUserId() != null) {
            return String.valueOf(requestParam.getUserId());
        }
        return UserContext.getUserId();
    }
}
