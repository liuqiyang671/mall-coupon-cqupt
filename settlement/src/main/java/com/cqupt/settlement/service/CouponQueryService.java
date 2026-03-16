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
     * 查询用户可/不可用的优惠券列表
     * @param  requestParam
     * @return 可/不可用的优惠券列表
     */
    CompletableFuture<CouponsRespDTO> pageQueryUserCoupons(QueryCouponsReqDTO requestParam);
}

