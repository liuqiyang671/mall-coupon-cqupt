package com.mall.cqupt.merchant.admin.user;

import com.mall.cqupt.merchant.admin.config.JWTUtil;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void testBCryptEncodeAndMatch() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Test@123456";
        String encodedPassword = encoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encoder.matches(rawPassword, encodedPassword));
        assertFalse(encoder.matches("WrongPassword", encodedPassword));
    }

    @Test
    void testBCryptDifferentEncodings() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "Test@123456";
        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        assertNotEquals(encoded1, encoded2);
        assertTrue(encoder.matches(rawPassword, encoded1));
        assertTrue(encoder.matches(rawPassword, encoded2));
    }

    @Test
    void testJWTTokenContainsUserInfo() {
        JWTUtil jwtUtil = new JWTUtil();
        try {
            var secretField = JWTUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "one-coupon-merchant-admin-jwt-secret-key-2026");

            var expirationField = JWTUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, 86400000L);
        } catch (Exception e) {
            fail("初始化失败: " + e.getMessage());
        }

        UserInfoDTO userInfo = UserInfoDTO.builder()
                .userId("10001")
                .username("merchant001")
                .roleType(1)
                .shopNumber(1810714735922956666L)
                .build();

        String token = jwtUtil.generateToken(userInfo);
        UserInfoDTO parsed = jwtUtil.parseToken(token);

        assertEquals("10001", parsed.getUserId());
        assertEquals("merchant001", parsed.getUsername());
        assertEquals(1, parsed.getRoleType());
        assertEquals(1810714735922956666L, parsed.getShopNumber());
    }
}
