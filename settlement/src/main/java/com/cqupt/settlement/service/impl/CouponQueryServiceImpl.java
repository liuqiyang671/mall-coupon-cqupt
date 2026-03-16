package com.cqupt.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.cqupt.settlement.dao.entity.UserCouponDO;
import com.cqupt.settlement.dao.mapper.UserCouponMapper;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.CouponsRespDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;
import com.cqupt.settlement.service.CouponQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 查询用户可用 / 不可用优惠券列表接口
 */
@Service
@RequiredArgsConstructor
public class CouponQueryServiceImpl implements CouponQueryService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper; // 用于 JSON 序列化和反序列化
    private final StringRedisTemplate stringRedisTemplate;
    private static final String COUPON_CACHE_KEY_PREFIX = "user:coupons:";

    /**
     * 查询用户可用和不可用的优惠券列表，返回 CouponsRespDTO 对象
     *
     * @param requestParam 查询参数
     * @return CompletableFuture<CouponsRespDTO> 包含可用和不可用优惠券的分页结果
     */
    @Override
    public CompletableFuture<CouponsRespDTO> pageQueryUserCoupons(QueryCouponsReqDTO requestParam) {
        return CompletableFuture.supplyAsync(() -> {
            // 定义 Redis 操作对象
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();

            // 构造缓存键
            String cacheKey = COUPON_CACHE_KEY_PREFIX + requestParam.getUserId() + ":" + requestParam.getPageNum() + ":" + requestParam.getPageSize();

            CouponsRespDTO cachedCoupons = null;

            try {
                // 尝试从缓存获取所有优惠券（获取的是JSON字符串）
                String cachedJson = valueOps.get(cacheKey);
                if (cachedJson != null) {
                    // 将 JSON 字符串反序列化为 CouponsRespDTO 对象
                    cachedCoupons = objectMapper.readValue(cachedJson, CouponsRespDTO.class);
                }
            } catch (Exception e) {
                // 记录缓存获取时的异常信息
                System.err.println("Error retrieving from Redis: " + e.getMessage());
                e.printStackTrace();
            }

            // 如果缓存命中，直接返回
            if (cachedCoupons != null) {
                return cachedCoupons;
            }

            // 查询用户所有优惠券
            IPage<UserCouponDO> allCouponsPage = queryAllUserCoupons(requestParam);

            // 区分可用和不可用优惠券
            List<QueryCouponsRespDTO> availableCoupons = new ArrayList<>();
            List<QueryCouponsRespDTO> unavailableCoupons = new ArrayList<>();

            for (UserCouponDO coupon : allCouponsPage.getRecords()) {
                QueryCouponsRespDTO dto = convertToRespDTO(coupon);
                if (coupon.getStatus() == 0) { // 状态为0表示可用
                    availableCoupons.add(dto);
                } else { // 其他状态表示不可用
                    unavailableCoupons.add(dto);
                }
            }

            // 创建分页对象
            IPage<QueryCouponsRespDTO> availableCouponsPage = new Page<>(allCouponsPage.getCurrent(), allCouponsPage.getSize(), allCouponsPage.getTotal());
            availableCouponsPage.setRecords(availableCoupons);

            IPage<QueryCouponsRespDTO> unavailableCouponsPage = new Page<>(allCouponsPage.getCurrent(), allCouponsPage.getSize(), allCouponsPage.getTotal());
            unavailableCouponsPage.setRecords(unavailableCoupons);

            // 构造返回对象
            CouponsRespDTO response = CouponsRespDTO.builder()
                    .availableCoupons(availableCouponsPage)
                    .unavailableCoupons(unavailableCouponsPage)
                    .build();

            try {
                // 将 CouponsRespDTO 对象序列化为 JSON 字符串
                String responseJson = objectMapper.writeValueAsString(response);

                valueOps.set(cacheKey, responseJson);
                // 缓存结果并设置失效时间为 1 小时
                // valueOps.set(cacheKey, response, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                // 记录缓存存储时的异常信息
                System.err.println("Error storing to Redis: " + e.getMessage());
                e.printStackTrace();
            }

            return response;
        });
    }

    /**
     * 分页查询用户所有优惠券
     *
     * @param requestParam 查询参数
     * @return 用户优惠券的分页结果
     */
    private IPage<UserCouponDO> queryAllUserCoupons(QueryCouponsReqDTO requestParam) {
        // 创建分页对象
        Page<UserCouponDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());

        // 创建查询条件
        QueryWrapper<UserCouponDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", requestParam.getUserId())
                .orderByDesc("id");

        // 执行分页查询
        return userCouponMapper.selectPage(page, queryWrapper);
    }

    /**
     * 转换 UserCouponDO 对象为 QueryCouponsRespDTO
     *
     * @param userCoupon 用户优惠券对象
     * @return 响应DTO
     */
    private QueryCouponsRespDTO convertToRespDTO(UserCouponDO userCoupon) {
        return QueryCouponsRespDTO.builder()
                .couponTemplateId(userCoupon.getCouponTemplateId())
                .receiveTime(userCoupon.getReceiveTime())
                .validStartTime(userCoupon.getValidStartTime())
                .validEndTime(userCoupon.getValidEndTime())
                .status(userCoupon.getStatus())
                .build();
    }
}