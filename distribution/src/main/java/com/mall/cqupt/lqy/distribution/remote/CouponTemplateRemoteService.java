package com.mall.cqupt.lqy.distribution.remote;


import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.lqy.distribution.remote.dto.req.CouponTemplateQueryRemoteReqDTO;
import com.mall.cqupt.lqy.distribution.remote.dto.resp.CouponTemplateQueryRemoteRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 引擎服务优惠券模板远程调用
 */
@FeignClient(value = "oneCoupon-engine")
public interface CouponTemplateRemoteService {

    /**
     * 查询优惠券模板
     */
    @GetMapping("/api/engine/coupon-template/query")
    Result<CouponTemplateQueryRemoteRespDTO> pageQueryCouponTemplate(CouponTemplateQueryRemoteReqDTO requestParam);
}
