package com.mall.cqupt.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TokenValidateFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret:one-coupon-merchant-admin-jwt-secret-key-2026}")
    private String secret;

    private static final List<String> WHITE_PATH_LIST = List.of(
            "/api/merchant-admin/user/register",
            "/api/merchant-admin/user/login",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange.getResponse(), "未登录或登录已过期");
        }

        String token = authorization.substring(7);
        try {
            JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret)).build();
            verifier.verify(token);

            String userId = JWT.decode(token).getClaim("userId").asString();
            String username = JWT.decode(token).getClaim("username").asString();
            String shopNumber = JWT.decode(token).getClaim("shopNumber").asString();

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-Username", username)
                    .header("X-Shop-Number", shopNumber)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (JWTVerificationException e) {
            log.warn("Gateway JWT 验证失败, path: {}, message: {}", path, e.getMessage());
            return unauthorized(exchange.getResponse(), "登录已过期，请重新登录");
        }
    }

    private boolean isWhitePath(String path) {
        return WHITE_PATH_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> result = Map.of(
                "code", "A000001",
                "message", message,
                "data", "",
                "requestId", ""
        );
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":\"A000001\",\"message\":\"未登录或登录已过期\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
