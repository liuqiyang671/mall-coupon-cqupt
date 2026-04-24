package com.mall.cqupt.merchant.admin.user;

import com.mall.cqupt.framework.exception.ClientException;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.config.JWTUtil;
import com.mall.cqupt.merchant.admin.dao.entity.UserDO;
import com.mall.cqupt.merchant.admin.dao.mapper.UserMapper;
import com.mall.cqupt.merchant.admin.dto.req.UserChangePasswordReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserLoginReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserRegisterReqDTO;
import com.mall.cqupt.merchant.admin.dto.req.UserUpdateReqDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserInfoRespDTO;
import com.mall.cqupt.merchant.admin.dto.resp.UserLoginRespDTO;
import com.mall.cqupt.merchant.admin.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserServiceImpl userService;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserDO mockUserDO;

    @BeforeEach
    void setUp() {
        mockUserDO = UserDO.builder()
                .id(1L)
                .roleType(1)
                .username("testuser")
                .password(passwordEncoder.encode("Test@123456"))
                .nickname("测试商家")
                .phone("13800138000")
                .mail("test@example.com")
                .shopNumber("1810714735922956666")
                .status(0)
                .activationStatus(1)
                .delFlag(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void testRegister_Success() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("newuser")
                .password("Test@123456")
                .phone("13900139000")
                .mail("new@example.com")
                .build();

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(UserDO.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.register(reqDTO));
        ArgumentCaptor<UserDO> userCaptor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(1, userCaptor.getValue().getRoleType());
        assertNotNull(userCaptor.getValue().getShopNumber());
        assertEquals(0, userCaptor.getValue().getStatus());
        assertEquals(1, userCaptor.getValue().getActivationStatus());
    }

    @Test
    void testRegister_CustomerSuccessWithoutShopNumber() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .roleType(2)
                .username("customer001")
                .password("Test@123456")
                .phone("13900139002")
                .mail("customer@example.com")
                .build();

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(UserDO.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.register(reqDTO));
        ArgumentCaptor<UserDO> userCaptor = ArgumentCaptor.forClass(UserDO.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(2, userCaptor.getValue().getRoleType());
        assertNull(userCaptor.getValue().getShopNumber());
    }

    @Test
    void testRegister_DuplicateUsername() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("testuser")
                .password("Test@123456")
                .build();

        when(userMapper.selectOne(any())).thenReturn(mockUserDO);

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("用户名已存在"));
    }

    @Test
    void testRegister_BlankUsername() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("")
                .password("Test@123456")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("用户名不能为空"));
    }

    @Test
    void testRegister_ShortPassword() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("newuser")
                .password("12345")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("密码长度"));
    }

    @Test
    void testRegister_InvalidPhone() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("newuser")
                .password("Test@123456")
                .phone("123")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("手机号格式"));
    }

    @Test
    void testRegister_InvalidMail() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .username("newuser")
                .password("Test@123456")
                .mail("invalid-email")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("邮箱格式"));
    }

    @Test
    void testRegister_InvalidMerchantShopNumber() {
        UserRegisterReqDTO reqDTO = UserRegisterReqDTO.builder()
                .roleType(1)
                .username("merchant002")
                .password("Test@123456")
                .shopNumber("shop-no-001")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.register(reqDTO));
        assertTrue(exception.getMessage().contains("店铺编号必须为数字"));
    }

    @Test
    void testLogin_Success() {
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("testuser")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(mockUserDO);
        when(jwtUtil.generateToken(any())).thenReturn("mock.jwt.token");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);

        UserLoginRespDTO respDTO = userService.login(reqDTO);

        assertNotNull(respDTO);
        assertEquals("mock.jwt.token", respDTO.getToken());
        assertEquals("testuser", respDTO.getUsername());
        assertEquals("1", respDTO.getUserId());
        assertEquals(1, respDTO.getRoleType());
        assertEquals("测试商家", respDTO.getNickname());
    }

    @Test
    void testLogin_PlatformSuccess() {
        UserDO platformUser = UserDO.builder()
                .id(2L)
                .roleType(0)
                .username("platform_admin")
                .password(passwordEncoder.encode("Test@123456"))
                .nickname("平台管理员")
                .status(0)
                .activationStatus(1)
                .delFlag(0)
                .build();
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .roleType(0)
                .username("platform_admin")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(platformUser);
        when(jwtUtil.generateToken(any())).thenReturn("mock.platform.jwt.token");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);

        UserLoginRespDTO respDTO = userService.login(reqDTO);

        assertEquals("2", respDTO.getUserId());
        assertEquals(0, respDTO.getRoleType());
        assertNull(respDTO.getShopNumber());
    }

    @Test
    void testLogin_DisabledAccount() {
        mockUserDO.setStatus(1);
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("testuser")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(mockUserDO);

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("账号已被禁用"));
    }

    @Test
    void testLogin_InactiveAccount() {
        mockUserDO.setActivationStatus(0);
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("testuser")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(mockUserDO);

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("账号未激活"));
    }

    @Test
    void testLogin_WrongPassword() {
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("testuser")
                .password("WrongPassword")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(mockUserDO);

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testLogin_UserNotFound() {
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("nonexistent")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("用户名或密码错误"));
    }

    @Test
    void testLogin_AccountLocked() {
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("testuser")
                .password("Test@123456")
                .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("login-lock"))).thenReturn("1");

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("账号已被锁定"));
    }

    @Test
    void testLogin_BlankCredentials() {
        UserLoginReqDTO reqDTO = UserLoginReqDTO.builder()
                .username("")
                .password("")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.login(reqDTO));
        assertTrue(exception.getMessage().contains("用户名和密码不能为空"));
    }

    @Test
    void testGetUserInfo_Success() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        when(userMapper.selectById(1L)).thenReturn(mockUserDO);

        UserInfoRespDTO respDTO = userService.getUserInfo();
        assertNotNull(respDTO);
        assertEquals("1", respDTO.getUserId());
        assertEquals("testuser", respDTO.getUsername());
        assertEquals("13800138000", respDTO.getPhone());
    }

    @Test
    void testChangePassword_Success() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        UserChangePasswordReqDTO reqDTO = UserChangePasswordReqDTO.builder()
                .oldPassword("Test@123456")
                .newPassword("NewPass@789")
                .build();

        when(userMapper.selectById(1L)).thenReturn(mockUserDO);
        when(userMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.changePassword(reqDTO));
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        UserChangePasswordReqDTO reqDTO = UserChangePasswordReqDTO.builder()
                .oldPassword("WrongOldPass")
                .newPassword("NewPass@789")
                .build();

        when(userMapper.selectById(1L)).thenReturn(mockUserDO);

        ClientException exception = assertThrows(ClientException.class, () -> userService.changePassword(reqDTO));
        assertTrue(exception.getMessage().contains("原密码错误"));
    }

    @Test
    void testChangePassword_SamePassword() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        UserChangePasswordReqDTO reqDTO = UserChangePasswordReqDTO.builder()
                .oldPassword("SamePass@1")
                .newPassword("SamePass@1")
                .build();

        ClientException exception = assertThrows(ClientException.class, () -> userService.changePassword(reqDTO));
        assertTrue(exception.getMessage().contains("新密码不能与原密码相同"));
    }

    @Test
    void testUpdateUserInfo_Success() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        UserUpdateReqDTO reqDTO = UserUpdateReqDTO.builder()
                .phone("13900139001")
                .mail("updated@example.com")
                .build();

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateUserInfo(reqDTO));
    }

    @Test
    void testLogout_Success() {
        try {
            var userContextClass = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserContext");
            var setUserMethod = userContextClass.getDeclaredMethod("setUser",
                    Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO"));
            var userInfoDTO = Class.forName("com.mall.cqupt.merchant.admin.common.context.UserInfoDTO")
                    .getDeclaredConstructor(String.class, String.class, Long.class)
                    .newInstance("1", "testuser", 1810714735922956666L);
            setUserMethod.invoke(null, userInfoDTO);
        } catch (Exception e) {
            fail("设置 UserContext 失败: " + e.getMessage());
        }

        assertDoesNotThrow(() -> userService.logout());
    }
}
