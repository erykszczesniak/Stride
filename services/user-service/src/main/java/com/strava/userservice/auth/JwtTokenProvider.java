package com.strava.userservice.auth;

import com.strava.userservice.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-ttl-seconds:3600}") long accessTtl,
            @Value("${jwt.refresh-token-ttl-seconds:604800}") long refreshTtl) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTtl;
        this.refreshTokenTtlSeconds = refreshTtl;
    }

    public TokenPair issueTokens(Long userId, String email, Role role) {
        Date now = new Date();
        String access = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("email", email)
                .claim("roles", List.of(role.name()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtlSeconds * 1000))
                .signWith(key)
                .compact();
        String refresh = Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("email", email)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenTtlSeconds * 1000))
                .signWith(key)
                .compact();
        return new TokenPair(access, refresh, accessTokenTtlSeconds);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}
}
