package cn.mall.cqupt.merchant.admin.service;

import cn.mall.cqupt.merchant.admin.dao.entity.CouponTemplateDO;
import cn.mall.cqupt.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 新增商家优惠券模板
     *
     * @param requestParam 请求参数
     */
    void saveCouponTemplate(CouponTemplateSaveReqDTO requestParam);
}
