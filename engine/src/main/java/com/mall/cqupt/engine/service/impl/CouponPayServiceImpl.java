package com.mall.cqupt.engine.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mall.cqupt.engine.common.constant.EngineRedisConstant;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.enums.UserCouponStatusEnum;
import com.mall.cqupt.engine.dao.entity.CouponSettlementDO;
import com.mall.cqupt.engine.dao.entity.UserCouponDO;
import com.mall.cqupt.engine.dao.mapper.CouponSettlementMapper;
import com.mall.cqupt.engine.dao.mapper.UserCouponMapper;
import com.mall.cqupt.engine.dto.req.*;
import com.mall.cqupt.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.mall.cqupt.engine.service.CouponPayService;
import com.mall.cqupt.engine.service.CouponTemplateService;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * 优惠券支付服务相关接口层实现

 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponPayServiceImpl implements CouponPayService {

    private final CouponTemplateService couponTemplateService;
    private final UserCouponMapper userCouponMapper;
    private final CouponSettlementMapper couponSettlementMapper;
    private final RedissonClient redissonClient;

    private final TransactionTemplate transactionTemplate;

    @Override
    public void createPaymentRecord(CouponCreatePaymentReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在创建优惠券结算单，请稍候再试");
        }
        try {
            LambdaQueryWrapper<CouponSettlementDO> queryWrapper = Wrappers.lambdaQuery(CouponSettlementDO.class)
                    .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                    .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                    .in(CouponSettlementDO::getStatus, 0, 2);

            // 验证优惠券是否正在使用或者已经被使用
            if (couponSettlementMapper.selectOne(queryWrapper) != null) {
                throw new ClientException("请检查优惠券是否已使用");
            }

            UserCouponDO userCouponDO = userCouponMapper.selectOne(Wrappers.lambdaQuery(UserCouponDO.class)
                    .eq(UserCouponDO::getId, requestParam.getCouponId())
                    .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId())));

            // 验证用户优惠券状态和有效性
            if (Objects.isNull(userCouponDO)) {
                throw new ClientException("优惠券不存在");
            }
            if (userCouponDO.getValidEndTime().before(new Date())) {
                throw new ClientException("优惠券已过期");
            }
            if (userCouponDO.getStatus() != 0) {
                throw new ClientException("优惠券使用状态异常");
            }

            // 获取优惠券模板和消费规则
            CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(
                    new CouponTemplateQueryReqDTO(requestParam.getShopNumber(), String.valueOf(userCouponDO.getCouponTemplateId())));
            JSONObject consumeRule = JSONObject.parseObject(couponTemplate.getConsumeRule());

            // 计算折扣金额
            BigDecimal discountAmount;

            // 商品专属优惠券
            if (couponTemplate.getTarget().equals(0)) {
                // 获取第一个匹配的商品
                Optional<CouponCreatePaymentGoodsReqDTO> matchedGoods = requestParam.getGoodsList().stream()
                        .filter(each -> Objects.equals(couponTemplate.getGoods(), each.getGoodsNumber()))
                        .findFirst();

                if (matchedGoods.isEmpty()) {
                    throw new ClientException("商品信息与优惠券模板不符");
                }

                // 验证折扣金额
                CouponCreatePaymentGoodsReqDTO paymentGoods = matchedGoods.get();
                BigDecimal maximumDiscountAmount = consumeRule.getBigDecimal("maximumDiscountAmount");
                if (!paymentGoods.getGoodsAmount().subtract(maximumDiscountAmount).equals(paymentGoods.getGoodsPayableAmount())) {
                    throw new ClientException("商品折扣后金额异常");
                }

                discountAmount = maximumDiscountAmount;
            } else { // 店铺专属
                // 检查店铺编号（如果是店铺券）
                if (couponTemplate.getSource() == 0 && !requestParam.getShopNumber().equals(couponTemplate.getShopNumber())) {
                    throw new ClientException("店铺编号不一致");
                }

                BigDecimal termsOfUse = consumeRule.getBigDecimal("termsOfUse");
                if (requestParam.getOrderAmount().compareTo(termsOfUse) < 0) {
                    throw new ClientException("订单金额未满足使用条件");
                }

                BigDecimal maximumDiscountAmount = consumeRule.getBigDecimal("maximumDiscountAmount");

                switch (couponTemplate.getType()) {
                    case 0: // 立减券
                        discountAmount = maximumDiscountAmount;
                        break;
                    case 1: // 满减券
                        discountAmount = maximumDiscountAmount;
                        break;
                    case 2: // 折扣券
                        BigDecimal discountRate = consumeRule.getBigDecimal("discountRate");
                        discountAmount = requestParam.getOrderAmount().multiply(discountRate);
                        break;
                    default:
                        throw new ClientException("无效的优惠券类型");
                }
            }

            // 计算折扣后金额并进行检查
            BigDecimal actualPayableAmount = requestParam.getOrderAmount().subtract(discountAmount);
            if (actualPayableAmount.compareTo(requestParam.getPayableAmount()) != 0) {
                throw new ClientException("折扣后金额不一致");
            }

            // 通过编程式事务减小事务范围
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    // 创建优惠券结算单记录
                    CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                            .orderId(requestParam.getOrderId())
                            .couponId(requestParam.getCouponId())
                            .userId(Long.parseLong(UserContext.getUserId()))
                            .status(0)
                            .build();
                    couponSettlementMapper.insert(couponSettlementDO);

                    // 变更用户优惠券状态
                    LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                            .eq(UserCouponDO::getId, requestParam.getCouponId())
                            .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                            .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode());
                    UserCouponDO updateUserCouponDO = UserCouponDO.builder()
                            .status(UserCouponStatusEnum.LOCKING.getCode())
                            .build();
                    userCouponMapper.update(updateUserCouponDO, userCouponUpdateWrapper);
                } catch (Exception ex) {
                    log.error("创建优惠券结算单失败", ex);
                    status.setRollbackOnly();
                    throw ex;
                }
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void processPayment(CouponProcessPaymentReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在核销优惠券结算单，请稍候再试");
        }

        // 通过编程式事务减小事务范围
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 变更优惠券结算单状态为已支付
                LambdaUpdateWrapper<CouponSettlementDO> couponSettlementUpdateWrapper = Wrappers.lambdaUpdate(CouponSettlementDO.class)
                        .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                        .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(CouponSettlementDO::getStatus, 0);
                CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                        .status(2)
                        .build();
                int couponSettlementUpdated = couponSettlementMapper.update(couponSettlementDO, couponSettlementUpdateWrapper);
                if (!SqlHelper.retBool(couponSettlementUpdated)) {
                    log.error("核销优惠券结算单异常，请求参数：{}", JSON.toJSONString(requestParam));
                    throw new ServiceException("核销优惠券结算单异常");
                }

                // 变更用户优惠券状态
                LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                        .eq(UserCouponDO::getId, requestParam.getCouponId())
                        .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(UserCouponDO::getStatus, UserCouponStatusEnum.LOCKING.getCode());
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .status(UserCouponStatusEnum.USED.getCode())
                        .build();
                int userCouponUpdated = userCouponMapper.update(userCouponDO, userCouponUpdateWrapper);
                if (!SqlHelper.retBool(userCouponUpdated)) {
                    log.error("修改用户优惠券记录状态已使用异常，请求参数：{}", JSON.toJSONString(requestParam));
                    throw new ServiceException("修改用户优惠券记录状态异常");
                }
            } catch (Exception ex) {
                log.error("核销优惠券结算单失败", ex);
                status.setRollbackOnly();
                throw ex;
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public void processRefund(CouponProcessRefundReqDTO requestParam) {
        RLock lock = redissonClient.getLock(String.format(EngineRedisConstant.LOCK_COUPON_SETTLEMENT_KEY, requestParam.getCouponId()));
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ClientException("正在执行优惠券退款，请稍候再试");
        }

        // 通过编程式事务减小事务范围
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // 变更优惠券结算单状态为已退款
                LambdaUpdateWrapper<CouponSettlementDO> couponSettlementUpdateWrapper = Wrappers.lambdaUpdate(CouponSettlementDO.class)
                        .eq(CouponSettlementDO::getCouponId, requestParam.getCouponId())
                        .eq(CouponSettlementDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(CouponSettlementDO::getStatus, 2);
                CouponSettlementDO couponSettlementDO = CouponSettlementDO.builder()
                        .status(3)
                        .build();
                int couponSettlementUpdated = couponSettlementMapper.update(couponSettlementDO, couponSettlementUpdateWrapper);
                if (!SqlHelper.retBool(couponSettlementUpdated)) {
                    log.error("优惠券结算单退款异常，请求参数：{}", JSON.toJSONString(requestParam));
                    throw new ServiceException("核销优惠券结算单异常");
                }

                // 变更用户优惠券状态
                LambdaUpdateWrapper<UserCouponDO> userCouponUpdateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                        .eq(UserCouponDO::getId, requestParam.getCouponId())
                        .eq(UserCouponDO::getUserId, Long.parseLong(UserContext.getUserId()))
                        .eq(UserCouponDO::getStatus, UserCouponStatusEnum.USED.getCode());
                UserCouponDO userCouponDO = UserCouponDO.builder()
                        .status(UserCouponStatusEnum.UNUSED.getCode())
                        .build();
                int userCouponUpdated = userCouponMapper.update(userCouponDO, userCouponUpdateWrapper);
                if (!SqlHelper.retBool(userCouponUpdated)) {
                    log.error("修改用户优惠券记录状态未使用异常，请求参数：{}", JSON.toJSONString(requestParam));
                    throw new ServiceException("修改用户优惠券记录状态异常");
                }
            } catch (Exception ex) {
                log.error("执行优惠券结算单退款失败", ex);
                status.setRollbackOnly();
                throw ex;
            } finally {
                lock.unlock();
            }
        });
    }
}