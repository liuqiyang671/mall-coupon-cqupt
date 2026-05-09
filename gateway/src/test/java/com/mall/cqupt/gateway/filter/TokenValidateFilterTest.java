package com.mall.cqupt.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.mock.http.server.reactive.MockServerHttpRequest.get;

class TokenValidateFilterTest {

    private static final String SECRET = "one-coupon-merchant-admin-jwt-secret-key-2026";

    private TokenValidateFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TokenValidateFilter();
        ReflectionTestUtils.setField(filter, "secret", SECRET);
    }

    @Test
    void whitePathPassesThroughWithoutAuthorizationHeader() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                get("/api/merchant-admin/user/login").build());
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertEquals(null, exchange.getResponse().getStatusCode());
    }

    @Test
    void missingAuthorizationHeaderReturnsUnauthorized() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                get("/api/merchant-admin/coupon-template/page").build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void validTokenAddsUserHeadersBeforeForwarding() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                get("/api/merchant-admin/coupon-template/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
                        .build());
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("1", captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("merchant", captor.getValue().getRequest().getHeaders().getFirst("X-Username"));
        assertEquals("10001", captor.getValue().getRequest().getHeaders().getFirst("X-Shop-Number"));
    }

    @Test
    void invalidTokenReturnsUnauthorized() {
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                get("/api/merchant-admin/coupon-template/page")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
                        .build());

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    private String validToken() {
        return JWT.create()
                .withClaim("userId", "1")
                .withClaim("username", "merchant")
                .withClaim("shopNumber", "10001")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000L))
                .sign(Algorithm.HMAC256(SECRET));
    }
}
