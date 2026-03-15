package com.cqupt.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cqupt.settlement.dto.req.QueryCouponsReqDTO;
import com.cqupt.settlement.dto.resp.QueryCouponsRespDTO;


/**
 * 查询用户可用 / 不可用优惠券列表接口
 */
public interface CouponQueryService {

    /**
     * 查询用户可用的优惠券列表
     * @param  requestParam
     * @return 可用的优惠券列表
     */
    IPage<QueryCouponsRespDTO> pageQueryAvailableCoupons(QueryCouponsReqDTO requestParam);

    /**
     * 查询用户不可用的优惠券列表
     * @param requestParam
     * @return 不可用的优惠券列表
     */
    IPage<QueryCouponsRespDTO> pageQueryUnavailableCoupons(QueryCouponsReqDTO requestParam);
}

