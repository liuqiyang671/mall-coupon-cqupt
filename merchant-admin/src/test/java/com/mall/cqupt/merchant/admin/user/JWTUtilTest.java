package com.mall.cqupt.merchant.admin.user;

import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.config.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JWTUtilTest {

    private JWTUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil();
        try {
            var secretField = JWTUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "one-coupon-merchant-admin-jwt-secret-key-2026");

            var expirationField = JWTUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, 86400000L);
        } catch (Exception e) {
            fail("初始化 JWTUtil 失败: " + e.getMessage());
        }
    }

    @Test
    void testGenerateAndParseToken() {
        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId("1234567890")
                .username("testuser")
                .roleType(1)
                .shopNumber(1810714735922956666L)
                .build();

        String token = jwtUtil.generateToken(userInfoDTO);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        UserInfoDTO parsedUser = jwtUtil.parseToken(token);
        assertNotNull(parsedUser);
        assertEquals("1234567890", parsedUser.getUserId());
        assertEquals("testuser", parsedUser.getUsername());
        assertEquals(1, parsedUser.getRoleType());
        assertEquals(1810714735922956666L, parsedUser.getShopNumber());
    }

    @Test
    void testGenerateAndParseCustomerToken() {
        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId("20001")
                .username("customer001")
                .roleType(2)
                .build();

        String token = jwtUtil.generateToken(userInfoDTO);
        UserInfoDTO parsedUser = jwtUtil.parseToken(token);

        assertNotNull(parsedUser);
        assertEquals("20001", parsedUser.getUserId());
        assertEquals("customer001", parsedUser.getUsername());
        assertEquals(2, parsedUser.getRoleType());
        assertNull(parsedUser.getShopNumber());
    }

    @Test
    void testValidateToken_Valid() {
        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId("1234567890")
                .username("testuser")
                .roleType(1)
                .shopNumber(1810714735922956666L)
                .build();

        String token = jwtUtil.generateToken(userInfoDTO);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }

    @Test
    void testValidateToken_Empty() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void testParseToken_InvalidToken() {
        assertNull(jwtUtil.parseToken("invalid.token.string"));
    }

    @Test
    void testParseToken_TamperedToken() {
        UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                .userId("1234567890")
                .username("testuser")
                .roleType(1)
                .shopNumber(1810714735922956666L)
                .build();

        String token = jwtUtil.generateToken(userInfoDTO);
        String tamperedToken = token + "tampered";
        assertNull(jwtUtil.parseToken(tamperedToken));
    }

    @Test
    void testGetExpiration() {
        assertEquals(86400000L, jwtUtil.getExpiration());
    }
}
