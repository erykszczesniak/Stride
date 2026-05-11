package com.strava.shared.security;

import java.util.Set;

public record JwtClaims(
        Long userId,
        String email,
        Set<String> roles
) {}
