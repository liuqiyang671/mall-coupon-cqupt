package com.mall.cqupt.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "用户注册请求参数")
public class UserRegisterReqDTO {

    @Schema(description = "用户名", example = "merchant001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "用户角色 0：平台人员 1：商家 2：普通用户", example = "1")
    private Integer roleType;

    @Schema(description = "密码", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "昵称", example = "重邮优惠券商家")
    private String nickname;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "店铺编号，商家角色可传；为空时系统自动生成", example = "1810714735922956666")
    private String shopNumber;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "merchant@example.com")
    private String mail;
}
