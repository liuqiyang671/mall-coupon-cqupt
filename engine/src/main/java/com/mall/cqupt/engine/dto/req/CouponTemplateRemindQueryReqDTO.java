package com.mall.cqupt.engine.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分页查询抢券预约提醒接口请求参数实体
 */
@Data
@Schema(description = "分页查询优惠券预约抢券提醒参数实体")
public class CouponTemplateRemindQueryReqDTO extends Page {


    /**
     * 用户id
     */
    @Schema(description = "用户id", example = "1810868149847928832", required = true)
    private String userId;

}
