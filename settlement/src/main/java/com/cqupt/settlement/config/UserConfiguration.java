package com.cqupt.settlement.config;

import com.cqupt.settlement.common.context.UserContext;
import com.cqupt.settlement.common.context.UserInfoDTO;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用户信息透传配置
 */
@Configuration
public class UserConfiguration implements WebMvcConfigurer {

    @Bean
    public UserTransmitInterceptor userTransmitInterceptor() {
        return new UserTransmitInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor())
                .addPathPatterns("/**");
    }

    static class UserTransmitInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler) {
            String userId = request == null ? null : request.getHeader("X-User-Id");
            String username = request == null ? null : request.getHeader("X-Username");
            String shopNumber = request == null ? null : request.getHeader("X-Shop-Number");
            UserInfoDTO userInfoDTO = userId == null || userId.isBlank()
                    ? new UserInfoDTO("1810518709471555585", "pdd45305558318", 1810714735922956666L)
                    : new UserInfoDTO(userId, username, shopNumber == null || shopNumber.isBlank() ? null : Long.parseLong(shopNumber));
            UserContext.setUser(userInfoDTO);
            return true;
        }

        @Override
        public void afterCompletion(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable Object handler, Exception exception) {
            UserContext.removeUser();
        }
    }
}
