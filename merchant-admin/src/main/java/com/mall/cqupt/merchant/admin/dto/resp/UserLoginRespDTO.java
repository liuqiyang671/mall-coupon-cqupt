package com.mall.cqupt.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "用户登录响应参数")
public class UserLoginRespDTO {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "Token 过期时间（毫秒时间戳）")
    private Long expireTime;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户角色 0：平台人员 1：商家 2：普通用户")
    private Integer roleType;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "店铺编号")
    private Long shopNumber;

    @Schema(description = "账号状态 0：正常 1：禁用")
    private Integer status;

    @Schema(description = "激活状态 0：未激活 1：已激活")
    private Integer activationStatus;
}
