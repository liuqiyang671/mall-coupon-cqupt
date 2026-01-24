package com.mall.cqupt.merchant.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.cqupt.merchant.admin.dao.entity.CouponTaskDO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.CouponTaskPageQueryReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.CouponTaskPageQueryRespDTO;

/**
 * 优惠券推送业务逻辑层
 */
public interface CouponTaskService extends IService<CouponTaskDO> {

    /**
     * 商家创建优惠券推送任务
     *
     * @param requestParam 请求参数
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);

    /**
     * 分页查询商家优惠券推送任务
     *
     * @param requestParam 请求参数
     * @return 商家优惠券推送任务分页数据
     */
    IPage<CouponTaskPageQueryRespDTO> pageQueryCouponTask(CouponTaskPageQueryReqDTO requestParam);
}
