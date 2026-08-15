package com.strava.gateway;

import com.strava.shared.security.JwtClaims;
import com.strava.shared.security.JwtParser;
import com.strava.shared.security.UserContext;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Validates JWT on protected routes, propagates user identity to downstream services
 * as X-User-Id / X-User-Email / X-User-Roles headers, strips any spoofed copies of
 * those headers from inbound requests.
 *
 * Public paths (login/register/refresh/oauth2/actuator) bypass auth.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer ";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final JwtParser jwtParser;
    private final List<String> publicPaths;
    private final List<String> queryTokenPaths;

    public JwtAuthenticationFilter(JwtParser jwtParser,
                                   @org.springframework.beans.factory.annotation.Value("${jwt.public-paths:}") List<String> publicPaths,
                                   @org.springframework.beans.factory.annotation.Value("${jwt.query-token-paths:}") List<String> queryTokenPaths) {
        this.jwtParser = jwtParser;
        this.publicPaths = publicPaths;
        this.queryTokenPaths = queryTokenPaths;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Always strip header spoofing attempts before we make our own decision.
        ServerHttpRequest stripped = request.mutate()
                .headers(h -> {
                    h.remove(UserContext.HEADER_USER_ID);
                    h.remove(UserContext.HEADER_USER_EMAIL);
                    h.remove(UserContext.HEADER_USER_ROLES);
                })
                .build();

        if (isPublic(path)) {
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        String authHeader = stripped.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token;
        if (authHeader != null && authHeader.startsWith(BEARER)) {
            token = authHeader.substring(BEARER.length()).trim();
        } else {
            // EventSource cannot set headers, so SSE endpoints accept ?token= as a fallback.
            token = allowsQueryToken(path) ? request.getQueryParams().getFirst("token") : null;
            if (token == null || token.isBlank()) {
                return unauthorized(exchange, "Missing or malformed Authorization header");
            }
        }

        try {
            JwtClaims claims = jwtParser.parse(token);
            ServerHttpRequest enriched = stripped.mutate()
                    .header(UserContext.HEADER_USER_ID, String.valueOf(claims.userId()))
                    .header(UserContext.HEADER_USER_EMAIL, claims.email() != null ? claims.email() : "")
                    .header(UserContext.HEADER_USER_ROLES, String.join(",", claims.roles()))
                    .build();
            return chain.filter(exchange.mutate().request(enriched).build());
        } catch (JwtException e) {
            log.debug("JWT validation failed for {}: {}", path, e.getMessage());
            return unauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublic(String path) {
        for (String pattern : publicPaths) {
            if (MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    private boolean allowsQueryToken(String path) {
        for (String pattern : queryTokenPaths) {
            if (MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Reason", reason);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        // Run before any route filter so downstream forwarding sees the enriched headers.
        return -100;
    }
}
