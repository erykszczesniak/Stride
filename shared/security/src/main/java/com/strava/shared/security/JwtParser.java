package com.strava.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.jsonwebtoken.security.Keys;

public final class JwtParser {

    private final SecretKey key;

    public JwtParser(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public JwtClaims parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = claims.get("userId", Number.class) != null
                ? claims.get("userId", Number.class).longValue()
                : Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        Set<String> roles = extractRoles(claims.get("roles"));
        return new JwtClaims(userId, email, roles);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractRoles(Object raw) {
        if (raw == null) return Collections.emptySet();
        if (raw instanceof Collection<?> c) {
            Set<String> out = new LinkedHashSet<>();
            for (Object o : c) out.add(String.valueOf(o));
            return out;
        }
        if (raw instanceof String s) {
            Set<String> out = new LinkedHashSet<>();
            for (String r : s.split(",")) {
                String trimmed = r.trim();
                if (!trimmed.isEmpty()) out.add(trimmed);
            }
            return out;
        }
        return Collections.emptySet();
    }
}
