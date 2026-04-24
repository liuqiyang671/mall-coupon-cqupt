package com.mall.cqupt.merchant.admin.common.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录用户信息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoDTO {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色 0：平台人员 1：商家 2：普通用户
     */
    private Integer roleType;

    /**
     * 店铺编号
     */
    private Long shopNumber;

    public UserInfoDTO(String userId, String username, Long shopNumber) {
        this.userId = userId;
        this.username = username;
        this.roleType = 1;
        this.shopNumber = shopNumber;
    }
}
