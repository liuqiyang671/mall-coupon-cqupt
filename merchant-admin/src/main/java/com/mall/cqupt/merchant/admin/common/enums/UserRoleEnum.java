package com.mall.cqupt.merchant.admin.common.enums;

import com.mall.cqupt.framework.exception.ClientException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    PLATFORM(0, "平台人员"),
    MERCHANT(1, "商家"),
    CUSTOMER(2, "用户");

    private final Integer type;
    private final String description;

    public static UserRoleEnum fromType(Integer type) {
        return Arrays.stream(values())
                .filter(each -> each.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new ClientException("用户角色类型不正确"));
    }

    public static Integer defaultIfNull(Integer roleType) {
        return roleType == null ? MERCHANT.type : fromType(roleType).type;
    }
}
