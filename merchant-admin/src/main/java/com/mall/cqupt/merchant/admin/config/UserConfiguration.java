package com.mall.cqupt.merchant.admin.config;

import cn.hutool.core.util.StrUtil;
import com.mall.cqupt.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.mall.cqupt.merchant.admin.common.context.UserContext;
import com.mall.cqupt.merchant.admin.common.context.UserInfoDTO;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class UserConfiguration implements WebMvcConfigurer {

    private final JWTUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Bean
    public UserTransmitInterceptor userTransmitInterceptor() {
        return new UserTransmitInterceptor(jwtUtil, stringRedisTemplate);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/merchant-admin/user/register",
                        "/api/merchant-admin/user/login",
                        "/api/merchant-admin/auth/register",
                        "/api/merchant-admin/auth/login",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/knife4j/**",
                        "/webjars/**"
                );
    }

    @RequiredArgsConstructor
    static class UserTransmitInterceptor implements HandlerInterceptor {

        private final JWTUtil jwtUtil;
        private final StringRedisTemplate stringRedisTemplate;

        @Override
        public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) throws Exception {
            if (request == null) {
                return true;
            }
            String token = request.getHeader("Authorization");
            if (StrUtil.isNotBlank(token) && token.startsWith("Bearer ")) {
                token = token.substring(7);
                UserInfoDTO userInfoDTO = jwtUtil.parseToken(token);
                if (userInfoDTO != null && isSessionValid(userInfoDTO, token)) {
                    UserContext.setUser(userInfoDTO);
                    return true;
                }
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"A000001\",\"message\":\"未登录或登录已过期\",\"data\":null,\"requestId\":null}");
            return false;
        }

        private boolean isSessionValid(UserInfoDTO userInfoDTO, String token) {
            if (StrUtil.isBlank(userInfoDTO.getUserId()) || userInfoDTO.getRoleType() == null) {
                return false;
            }
            String tokenKey = String.format(MerchantAdminRedisConstant.USER_TOKEN_KEY, userInfoDTO.getRoleType(), userInfoDTO.getUserId());
            String savedToken = stringRedisTemplate.opsForValue().get(tokenKey);
            return StrUtil.equals(savedToken, token);
        }

        @Override
        public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, Exception exception) throws Exception {
            UserContext.removeUser();
        }
    }
}
