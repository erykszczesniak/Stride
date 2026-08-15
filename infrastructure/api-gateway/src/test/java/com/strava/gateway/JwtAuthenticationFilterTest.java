package com.strava.gateway;

import com.strava.shared.security.JwtParser;
import com.strava.shared.security.UserContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";

    private JwtAuthenticationFilter filter;
    private AtomicReference<ServerWebExchange> forwarded;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                new JwtParser(SECRET),
                List.of("/api/v1/auth/**", "/actuator/**", "/pipeline/v1/**"),
                List.of("/api/v1/notifications/stream"));
        forwarded = new AtomicReference<>();
        chain = exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    private String token(long userId) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("erik@example.com")
                .claims(Map.of("userId", userId, "email", "erik@example.com", "roles", List.of("USER")))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    @Test
    void publicPathPassesThroughWithoutIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header(UserContext.HEADER_USER_ID, "666"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(UserContext.HEADER_USER_ID)).isNull();
    }

    @Test
    void validBearerTokenIsExchangedForIdentityHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token(42L)));

        filter.filter(exchange, chain).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst(UserContext.HEADER_USER_ID)).isEqualTo("42");
        assertThat(headers.getFirst(UserContext.HEADER_USER_EMAIL)).isEqualTo("erik@example.com");
        assertThat(headers.getFirst(UserContext.HEADER_USER_ROLES)).isEqualTo("USER");
    }

    @Test
    void spoofedIdentityHeadersAreReplacedNotMerged() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header(UserContext.HEADER_USER_ID, "666")
                        .header("Authorization", "Bearer " + token(42L)));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get().getRequest().getHeaders().get(UserContext.HEADER_USER_ID))
                .containsExactly("42");
    }

    @Test
    void missingAuthorizationOnProtectedPathIs401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Auth-Reason")).isNotBlank();
    }

    @Test
    void garbageTokenIs401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me")
                        .header("Authorization", "Bearer not-a-jwt"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sseStreamPathAcceptsQueryToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notifications/stream?token=" + token(7L)));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(UserContext.HEADER_USER_ID))
                .isEqualTo("7");
    }

    @Test
    void queryTokenIsRejectedOutsideAllowedPaths() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me?token=" + token(7L)));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void pipelinePathBypassesJwtValidation() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pipeline/v1/dashboard")
                        .header("Authorization", "Bearer tri_not-a-jwt-token"));

        filter.filter(exchange, chain).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(UserContext.HEADER_USER_ID)).isNull();
    }
}
