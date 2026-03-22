package com.cqupt.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.CouponsRespDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;

import java.util.concurrent.CompletableFuture;


/**
 * 查询用户可用 / 不可用优惠券列表接口
 */
public interface CouponQueryService {

    /**
     * 查询用户可用/不可用的优惠券列表，返回 CouponsRespDTO 对象
     *
     * @param requestParam 查询参数
     * @return 包含可用/不可用优惠券的查询结果
     */
    QueryCouponsRespDTO listQueryUserCoupons(QueryCouponsReqDTO requestParam);

    /**
     * 查询用户可用/不可用的优惠券列表，返回 CouponsRespDTO 对象
     *
     * @param requestParam 查询参数
     * @return 包含可用/不可用优惠券的查询结果
     */
    QueryCouponsRespDTO listQueryUserCouponsBySync(QueryCouponsReqDTO requestParam);
}

