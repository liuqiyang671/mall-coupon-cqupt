package com.mall.cqupt.engine.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.cqupt.engine.common.context.UserContext;
import com.mall.cqupt.engine.common.context.UserInfoDTO;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 用户相关配置类
 */
@Slf4j
@Configuration
public class UserConfiguration implements WebMvcConfigurer {

    @Value("${jwt.secret:one-coupon-merchant-admin-jwt-secret-key-2026}")
    private String jwtSecret;

    /**
     * 用户信息传输拦截器
     */
    @Bean
    public UserTransmitInterceptor userTransmitInterceptor() {
        return new UserTransmitInterceptor(jwtSecret);
    }

    /**
     * 添加用户信息传递过滤器至相关路径拦截
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/webjars/**"
                );
    }

    /**
     * 用户信息传输拦截器
     * 从 Authorization 请求头解析 JWT token 获取用户信息
     */
    static class UserTransmitInterceptor implements HandlerInterceptor {

        private final String jwtSecret;
        private final ObjectMapper objectMapper = new ObjectMapper();

        UserTransmitInterceptor(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        @Override
        public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
            if (request == null) {
                return true;
            }

            // 从 Authorization 请求头解析 JWT token
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring(7);
                try {
                    JWTVerifier verifier = JWT.require(Algorithm.HMAC256(jwtSecret)).build();
                    DecodedJWT jwt = verifier.verify(token);
                    String userId = jwt.getClaim("userId").asString();
                    String username = jwt.getClaim("username").asString();
                    String shopNumberStr = jwt.getClaim("shopNumber").asString();
                    Long shopNumber = shopNumberStr != null && !shopNumberStr.isEmpty()
                            ? Long.parseLong(shopNumberStr) : null;
                    UserContext.setUser(new UserInfoDTO(userId, username, shopNumber));
                    return true;
                } catch (JWTVerificationException e) {
                    log.warn("JWT token 验证失败: {}", e.getMessage());
                }
            }

            // 未登录，返回 401
            if (response != null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                Map<String, Object> result = Map.of(
                        "code", "A000001",
                        "message", "未登录或登录已过期",
                        "data", "",
                        "requestId", ""
                );
                response.getWriter().write(objectMapper.writeValueAsString(result));
            }
            return false;
        }

        @Override
        public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, Exception exception) throws Exception {
            UserContext.removeUser();
        }
    }
}
