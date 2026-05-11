package com.strava.shared.security;

import java.util.Collections;
import java.util.Set;

public record UserContext(Long userId, String email, Set<String> roles) {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_EMAIL = "X-User-Email";
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public static UserContext anonymous() {
        return new UserContext(null, null, Collections.emptySet());
    }
}
