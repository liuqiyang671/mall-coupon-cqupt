package com.mall.cqupt.merchant.admin.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import com.mall.cqupt.merchant.admin.common.enums.UserRoleEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JWTUtil {

    @Value("${jwt.secret:one-coupon-merchant-admin-jwt-secret-key-2026}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    public String generateToken(UserInfoDTO userInfoDTO) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return JWT.create()
                .withClaim("userId", userInfoDTO.getUserId())
                .withClaim("username", userInfoDTO.getUsername())
                .withClaim("roleType", userInfoDTO.getRoleType() == null ? UserRoleEnum.MERCHANT.getType() : userInfoDTO.getRoleType())
                .withClaim("shopNumber", userInfoDTO.getShopNumber() != null ? userInfoDTO.getShopNumber().toString() : "")
                .withIssuedAt(now)
                .withExpiresAt(expireDate)
                .sign(Algorithm.HMAC256(secret));
    }

    public UserInfoDTO parseToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            DecodedJWT jwt = verifier.verify(token);
            String userId = jwt.getClaim("userId").asString();
            String username = jwt.getClaim("username").asString();
            Integer roleType = jwt.getClaim("roleType").asInt();
            String shopNumberStr = jwt.getClaim("shopNumber").asString();
            Long shopNumber = shopNumberStr != null && !shopNumberStr.isEmpty() ? Long.parseLong(shopNumberStr) : null;
            return UserInfoDTO.builder()
                    .userId(userId)
                    .username(username)
                    .roleType(roleType == null ? UserRoleEnum.MERCHANT.getType() : roleType)
                    .shopNumber(shopNumber)
                    .build();
        } catch (JWTVerificationException e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return true;
        } catch (JWTVerificationException e) {
            log.warn("JWT token 无效: {}", e.getMessage());
            return false;
        }
    }

    public long getExpiration() {
        return expiration;
    }
}
