package com.mall.cqupt.merchant.admin.controller;

import com.mall.cqupt.framework.idempotent.NoDuplicateSubmit;
import com.mall.cqupt.framework.result.Result;
import com.mall.cqupt.framework.web.Results;
import com.mall.cqupt.merchant.admin.dto.req.UserChangePasswordReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserLoginReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserRegisterReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserUpdateReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserInfoRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserLoginRespDTO;
import com.mall.cqupt.merchant.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交注册请求")
    @PostMapping("/api/merchant-admin/user/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/api/merchant-admin/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/api/merchant-admin/user/info")
    public Result<UserInfoRespDTO> getUserInfo() {
        return Results.success(userService.getUserInfo());
    }

    @Operation(summary = "修改用户信息")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交修改请求")
    @PostMapping("/api/merchant-admin/user/update")
    public Result<Void> updateUserInfo(@RequestBody UserUpdateReqDTO requestParam) {
        userService.updateUserInfo(requestParam);
        return Results.success();
    }

    @Operation(summary = "修改密码")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交修改密码请求")
    @PostMapping("/api/merchant-admin/user/change-password")
    public Result<Void> changePassword(@Valid @RequestBody UserChangePasswordReqDTO requestParam) {
        userService.changePassword(requestParam);
        return Results.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/api/merchant-admin/user/logout")
    public Result<Void> logout() {
        userService.logout();
        return Results.success();
    }
}
