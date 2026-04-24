package com.mall.cqupt.merchant.admin.service;

import com.mall.cqupt.merchant.admin.dto.req.UserChangePasswordReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserLoginReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserRegisterReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserUpdateReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserInfoRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserLoginRespDTO;

public interface UserService {

    void register(UserRegisterReqDTO requestParam);

    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    UserInfoRespDTO getUserInfo();

    void updateUserInfo(UserUpdateReqDTO requestParam);

    void changePassword(UserChangePasswordReqDTO requestParam);

    void logout();
}
