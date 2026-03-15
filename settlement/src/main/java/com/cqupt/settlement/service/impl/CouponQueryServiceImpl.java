package com.cqupt.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.cqupt.settlement.dao.entity.UserCouponDO;
import com.cqupt.settlement.dao.mapper.UserCouponMapper;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;
import com.cqupt.settlement.service.CouponQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询用户可用 / 不可用优惠券列表接口
 */
@Service
@RequiredArgsConstructor
public class CouponQueryServiceImpl implements CouponQueryService {

    @Autowired
    private UserCouponMapper userCouponMapper;

    /**
     * 查询用户可用的优惠券列表
     *
     * @param requestParam
     * @return 可用的优惠券列表
     */
    @Override
    public IPage<QueryCouponsRespDTO> pageQueryAvailableCoupons(QueryCouponsReqDTO requestParam) {
        
        // 分页对象
        Page<UserCouponDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());

        // 查询条件
        QueryWrapper<UserCouponDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", requestParam.getUserId())
                .eq("status", 0)  // 状态为0，表示未使用的优惠券
                .orderByDesc("id");

        // 执行分页查询
        IPage<UserCouponDO> couponPage = userCouponMapper.selectPage(page, queryWrapper);

        // 转换成响应DTO
        IPage<QueryCouponsRespDTO> result = couponPage.convert(userCoupon -> {
            return QueryCouponsRespDTO.builder()
                    .couponTemplateId(userCoupon.getCouponTemplateId())
                    .receiveTime(userCoupon.getReceiveTime())
                    .validStartTime(userCoupon.getValidStartTime())
                    .validEndTime(userCoupon.getValidEndTime())
                    .status(userCoupon.getStatus())
                    .build();
        });

        return result;
    }

    /**
     * 查询用户不可用的优惠券列表
     *
     * @param requestParam
     * @return 不可用的优惠券列表
     */
    @Override
    public IPage<QueryCouponsRespDTO> pageQueryUnavailableCoupons(QueryCouponsReqDTO requestParam) {
        // 分页对象
        Page<UserCouponDO> page = new Page<>(requestParam.getPageNum(), requestParam.getPageSize());

        // 查询条件
        QueryWrapper<UserCouponDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", requestParam.getUserId())
                .ne("status", 0)  // 状态不为0，表示不可用的优惠券
                .orderByDesc("id");

        // 执行分页查询
        IPage<UserCouponDO> couponPage = userCouponMapper.selectPage(page, queryWrapper);

        // 转换成响应DTO
        IPage<QueryCouponsRespDTO> result = couponPage.convert(userCoupon -> QueryCouponsRespDTO.builder()
                .couponTemplateId(userCoupon.getCouponTemplateId())
                .receiveTime(userCoupon.getReceiveTime())
                .validStartTime(userCoupon.getValidStartTime())
                .validEndTime(userCoupon.getValidEndTime())
                .status(userCoupon.getStatus())
                .build());

        return result;
    }
}
