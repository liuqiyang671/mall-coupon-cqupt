package com.mall.cqupt.engine.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户券包分页查询请求参数实体
 */
@Data
@Schema(description = "用户券包分页查询请求参数实体")
public class UserCouponPageQueryReqDTO extends Page {

    /**
     * 用户优惠券状态 0：未使用 1：锁定 2：已使用 3：已过期 4：已撤回
     */
    @Schema(description = "用户优惠券状态 0：未使用 1：锁定 2：已使用 3：已过期 4：已撤回")
    private Integer status;
}
