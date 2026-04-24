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
@Schema(description = "用户信息响应参数")
public class UserInfoRespDTO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户角色 0：平台人员 1：商家 2：普通用户")
    private Integer roleType;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "店铺编号")
    private String shopNumber;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String mail;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "账号状态 0：正常 1：禁用")
    private Integer status;

    @Schema(description = "激活状态 0：未激活 1：已激活")
    private Integer activationStatus;

    @Schema(description = "最后登录时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastLoginTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @Schema(description = "修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
