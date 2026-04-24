package com.mall.cqupt.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.framework.exception.ServiceException;
import com.mall.cqupt.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.UserActivationStatusEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import com.mall.cqupt.merchant.admin.common.enums.UserStatusEnum;
import com.mall.cqupt.merchant.admin.config.JWTUtil;
import com.mall.cqupt.merchant.admin.dao.entity.UserDO;
import com.mall.cqupt.merchant.admin.dao.mapper.UserMapper;
import com.mall.cqupt.merchant.admin.dto.req.UserChangePasswordReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserLoginReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserRegisterReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserUpdateReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserInfoRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserLoginRespDTO;
import com.mall.cqupt.merchant.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JWTUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final long LOGIN_LOCK_MINUTES = 30;
    private static final long LOGIN_FAIL_COUNT_MINUTES = 30;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 32;
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 32;

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        validateRegisterParam(requestParam);
        Integer roleType = UserRoleEnum.defaultIfNull(requestParam.getRoleType());

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getRoleType, roleType)
                .eq(UserDO::getDelFlag, 0);
        UserDO existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new ClientException("用户名已存在");
        }

        if (StrUtil.isNotBlank(requestParam.getPhone())) {
            LambdaQueryWrapper<UserDO> phoneQuery = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getPhone, requestParam.getPhone())
                    .eq(UserDO::getRoleType, roleType)
                    .eq(UserDO::getDelFlag, 0);
            if (userMapper.selectOne(phoneQuery) != null) {
                throw new ClientException("该手机号已被注册");
            }
        }

        if (StrUtil.isNotBlank(requestParam.getMail())) {
            LambdaQueryWrapper<UserDO> mailQuery = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getMail, requestParam.getMail())
                    .eq(UserDO::getRoleType, roleType)
                    .eq(UserDO::getDelFlag, 0);
            if (userMapper.selectOne(mailQuery) != null) {
                throw new ClientException("该邮箱已被注册");
            }
        }

        UserDO userDO = UserDO.builder()
                .roleType(roleType)
                .username(requestParam.getUsername())
                .password(passwordEncoder.encode(requestParam.getPassword()))
                .nickname(StrUtil.blankToDefault(requestParam.getNickname(), requestParam.getUsername()))
                .realName(requestParam.getRealName())
                .phone(requestParam.getPhone())
                .mail(requestParam.getMail())
                .shopNumber(buildShopNumber(roleType, requestParam.getShopNumber()))
                .status(UserStatusEnum.NORMAL.getStatus())
                .activationStatus(UserActivationStatusEnum.ACTIVE.getStatus())
                .build();
        int inserted = userMapper.insert(userDO);
        if (inserted <= 0) {
            throw new ServiceException("用户注册失败，请稍后重试");
        }
        log.info("用户注册成功，username: {}, userId: {}", requestParam.getUsername(), userDO.getId());
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getUsername()) || StrUtil.isBlank(requestParam.getPassword())) {
            throw new ClientException("用户名和密码不能为空");
        }
        Integer roleType = UserRoleEnum.defaultIfNull(requestParam.getRoleType());

        String lockKey = String.format(MerchantAdminRedisConstant.USER_LOGIN_LOCK_KEY, roleType, requestParam.getUsername());
        String lockValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (StrUtil.isNotBlank(lockValue)) {
            throw new ClientException("账号已被锁定，请" + LOGIN_LOCK_MINUTES + "分钟后再试");
        }

        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getRoleType, roleType)
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户名或密码错误");
        }
        if (userDO.getStatus() != null && !UserStatusEnum.NORMAL.getStatus().equals(userDO.getStatus())) {
            throw new ClientException("账号已被禁用，请联系管理员");
        }
        if (userDO.getActivationStatus() != null && !UserActivationStatusEnum.ACTIVE.getStatus().equals(userDO.getActivationStatus())) {
            throw new ClientException("账号未激活，请先完成账户激活");
        }

        if (!passwordEncoder.matches(requestParam.getPassword(), userDO.getPassword())) {
            handleLoginFail(roleType, requestParam.getUsername());
            throw new ClientException("用户名或密码错误");
        }

        clearLoginFailCount(roleType, requestParam.getUsername());

        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId(String.valueOf(userDO.getId()))
                .username(userDO.getUsername())
                .roleType(userDO.getRoleType())
                .shopNumber(userDO.getShopNumber() != null ? Long.parseLong(userDO.getShopNumber()) : null)
                .build();
        String token = jwtUtil.generateToken(userInfoDTO);

        String tokenKey = String.format(MerchantAdminRedisConstant.USER_TOKEN_KEY, userDO.getRoleType(), userDO.getId());
        stringRedisTemplate.opsForValue().set(tokenKey, token, jwtUtil.getExpiration(), TimeUnit.MILLISECONDS);
        UserDO loginUpdate = UserDO.builder()
                .id(userDO.getId())
                .lastLoginTime(new Date())
                .build();
        userMapper.updateById(loginUpdate);

        log.info("用户登录成功，username: {}, userId: {}", userDO.getUsername(), userDO.getId());

        return UserLoginRespDTO.builder()
                .token(token)
                .expireTime(System.currentTimeMillis() + jwtUtil.getExpiration())
                .userId(String.valueOf(userDO.getId()))
                .username(userDO.getUsername())
                .roleType(userDO.getRoleType())
                .nickname(userDO.getNickname())
                .shopNumber(userDO.getShopNumber() != null ? Long.parseLong(userDO.getShopNumber()) : null)
                .status(userDO.getStatus())
                .activationStatus(userDO.getActivationStatus())
                .build();
    }

    @Override
    public UserInfoRespDTO getUserInfo() {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }

        UserDO userDO = userMapper.selectById(Long.parseLong(userId));
        if (userDO == null || userDO.getDelFlag() == 1) {
            throw new ClientException("用户不存在");
        }
        ensureSameRole(userDO);

        UserInfoRespDTO respDTO = BeanUtil.toBean(userDO, UserInfoRespDTO.class);
        respDTO.setUserId(String.valueOf(userDO.getId()));
        return respDTO;
    }

    @Override
    public void updateUserInfo(UserUpdateReqDTO requestParam) {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }

        if (StrUtil.isNotBlank(requestParam.getPhone())) {
            LambdaQueryWrapper<UserDO> phoneQuery = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getPhone, requestParam.getPhone())
                    .eq(UserDO::getRoleType, UserContext.getRoleType())
                    .ne(UserDO::getId, Long.parseLong(userId))
                    .eq(UserDO::getDelFlag, 0);
            if (userMapper.selectOne(phoneQuery) != null) {
                throw new ClientException("该手机号已被其他用户使用");
            }
        }

        if (StrUtil.isNotBlank(requestParam.getMail())) {
            LambdaQueryWrapper<UserDO> mailQuery = Wrappers.lambdaQuery(UserDO.class)
                    .eq(UserDO::getMail, requestParam.getMail())
                    .eq(UserDO::getRoleType, UserContext.getRoleType())
                    .ne(UserDO::getId, Long.parseLong(userId))
                    .eq(UserDO::getDelFlag, 0);
            if (userMapper.selectOne(mailQuery) != null) {
                throw new ClientException("该邮箱已被其他用户使用");
            }
        }

        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, Long.parseLong(userId))
                .eq(UserDO::getRoleType, UserContext.getRoleType())
                .eq(UserDO::getDelFlag, 0);
        UserDO updateUserDO = new UserDO();
        if (StrUtil.isNotBlank(requestParam.getPhone())) {
            updateUserDO.setPhone(requestParam.getPhone());
        }
        if (StrUtil.isNotBlank(requestParam.getMail())) {
            updateUserDO.setMail(requestParam.getMail());
        }
        if (StrUtil.isNotBlank(requestParam.getNickname())) {
            updateUserDO.setNickname(requestParam.getNickname());
        }
        if (StrUtil.isNotBlank(requestParam.getRealName())) {
            updateUserDO.setRealName(requestParam.getRealName());
        }
        if (StrUtil.isNotBlank(requestParam.getAvatarUrl())) {
            updateUserDO.setAvatarUrl(requestParam.getAvatarUrl());
        }

        int updated = userMapper.update(updateUserDO, updateWrapper);
        if (updated <= 0) {
            throw new ServiceException("更新用户信息失败");
        }
        log.info("用户信息更新成功，userId: {}", userId);
    }

    @Override
    public void changePassword(UserChangePasswordReqDTO requestParam) {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户未登录");
        }

        if (requestParam.getNewPassword().length() < PASSWORD_MIN_LENGTH
                || requestParam.getNewPassword().length() > PASSWORD_MAX_LENGTH) {
            throw new ClientException("新密码长度必须在" + PASSWORD_MIN_LENGTH + "~" + PASSWORD_MAX_LENGTH + "位之间");
        }

        if (requestParam.getOldPassword().equals(requestParam.getNewPassword())) {
            throw new ClientException("新密码不能与原密码相同");
        }

        UserDO userDO = userMapper.selectById(Long.parseLong(userId));
        if (userDO == null || userDO.getDelFlag() == 1) {
            throw new ClientException("用户不存在");
        }
        ensureSameRole(userDO);

        if (!passwordEncoder.matches(requestParam.getOldPassword(), userDO.getPassword())) {
            throw new ClientException("原密码错误");
        }

        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getId, Long.parseLong(userId))
                .eq(UserDO::getRoleType, UserContext.getRoleType())
                .eq(UserDO::getDelFlag, 0);
        UserDO updateUserDO = UserDO.builder()
                .password(passwordEncoder.encode(requestParam.getNewPassword()))
                .build();
        int updated = userMapper.update(updateUserDO, updateWrapper);
        if (updated <= 0) {
            throw new ServiceException("修改密码失败");
        }

        String tokenKey = String.format(MerchantAdminRedisConstant.USER_TOKEN_KEY, UserContext.getRoleType(), userId);
        stringRedisTemplate.delete(tokenKey);

        log.info("用户修改密码成功，userId: {}", userId);
    }

    @Override
    public void logout() {
        String userId = UserContext.getUserId();
        if (StrUtil.isNotBlank(userId)) {
            String tokenKey = String.format(MerchantAdminRedisConstant.USER_TOKEN_KEY, UserContext.getRoleType(), userId);
            stringRedisTemplate.delete(tokenKey);
            log.info("用户退出登录，userId: {}", userId);
        }
        UserContext.removeUser();
    }

    private void validateRegisterParam(UserRegisterReqDTO requestParam) {
        UserRoleEnum.fromType(UserRoleEnum.defaultIfNull(requestParam.getRoleType()));
        if (StrUtil.isBlank(requestParam.getUsername())) {
            throw new ClientException("用户名不能为空");
        }
        if (requestParam.getUsername().length() < USERNAME_MIN_LENGTH
                || requestParam.getUsername().length() > USERNAME_MAX_LENGTH) {
            throw new ClientException("用户名长度必须在" + USERNAME_MIN_LENGTH + "~" + USERNAME_MAX_LENGTH + "位之间");
        }
        if (!requestParam.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            throw new ClientException("用户名只能包含字母、数字和下划线");
        }
        if (StrUtil.isBlank(requestParam.getPassword())) {
            throw new ClientException("密码不能为空");
        }
        if (requestParam.getPassword().length() < PASSWORD_MIN_LENGTH
                || requestParam.getPassword().length() > PASSWORD_MAX_LENGTH) {
            throw new ClientException("密码长度必须在" + PASSWORD_MIN_LENGTH + "~" + PASSWORD_MAX_LENGTH + "位之间");
        }
        if (StrUtil.isNotBlank(requestParam.getPhone()) && !requestParam.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new ClientException("手机号格式不正确");
        }
        if (StrUtil.isNotBlank(requestParam.getMail()) && !requestParam.getMail().matches("^[\\w.-]+@[\\w.-]+\\.\\w+$")) {
            throw new ClientException("邮箱格式不正确");
        }
        if (UserRoleEnum.MERCHANT.getType().equals(UserRoleEnum.defaultIfNull(requestParam.getRoleType()))
                && StrUtil.isNotBlank(requestParam.getShopNumber())
                && !requestParam.getShopNumber().matches("^\\d+$")) {
            throw new ClientException("店铺编号必须为数字");
        }
    }

    private String buildShopNumber(Integer roleType, String requestShopNumber) {
        if (!UserRoleEnum.MERCHANT.getType().equals(roleType)) {
            return null;
        }
        return StrUtil.blankToDefault(requestShopNumber, String.valueOf(IdUtil.getSnowflakeNextId()));
    }

    private void ensureSameRole(UserDO userDO) {
        Integer contextRoleType = UserContext.getRoleType();
        if (contextRoleType != null && !contextRoleType.equals(userDO.getRoleType())) {
            throw new ClientException("用户身份与当前登录角色不匹配");
        }
    }

    private void handleLoginFail(Integer roleType, String username) {
        String failCountKey = String.format(MerchantAdminRedisConstant.USER_LOGIN_FAIL_COUNT_KEY, roleType, username);
        String failCountStr = stringRedisTemplate.opsForValue().get(failCountKey);
        int failCount = failCountStr != null ? Integer.parseInt(failCountStr) + 1 : 1;

        stringRedisTemplate.opsForValue().set(failCountKey, String.valueOf(failCount), LOGIN_FAIL_COUNT_MINUTES, TimeUnit.MINUTES);

        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            String lockKey = String.format(MerchantAdminRedisConstant.USER_LOGIN_LOCK_KEY, roleType, username);
            stringRedisTemplate.opsForValue().set(lockKey, "1", LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
            log.warn("用户登录失败次数过多，账号锁定30分钟，username: {}", username);
        }
    }

    private void clearLoginFailCount(Integer roleType, String username) {
        String failCountKey = String.format(MerchantAdminRedisConstant.USER_LOGIN_FAIL_COUNT_KEY, roleType, username);
        stringRedisTemplate.delete(failCountKey);
    }
}
