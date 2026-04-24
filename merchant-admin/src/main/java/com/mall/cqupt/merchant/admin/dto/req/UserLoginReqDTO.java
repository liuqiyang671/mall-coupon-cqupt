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
@Schema(description = "用户登录请求参数")
public class UserLoginReqDTO {

    @Schema(description = "用户名", example = "merchant001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "用户角色 0：平台人员 1：商家 2：普通用户；不传默认商家", example = "1")
    private Integer roleType;

    @Schema(description = "密码", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
