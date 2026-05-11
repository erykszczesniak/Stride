package com.strava.shared.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads X-User-* headers propagated by the API gateway after JWT validation,
 * populates a thread-local UserContext for the duration of the request.
 *
 * Services trust these headers because the gateway is the only ingress point
 * and the headers are stripped from incoming external requests by gateway config.
 */
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) req;
        String idHeader = http.getHeader(UserContext.HEADER_USER_ID);
        try {
            if (idHeader != null && !idHeader.isBlank()) {
                Long userId = Long.parseLong(idHeader);
                String email = http.getHeader(UserContext.HEADER_USER_EMAIL);
                Set<String> roles = parseRoles(http.getHeader(UserContext.HEADER_USER_ROLES));
                UserContextHolder.set(new UserContext(userId, email, roles));
            }
            chain.doFilter(req, res);
        } finally {
            UserContextHolder.clear();
        }
    }

    private static Set<String> parseRoles(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String r : raw.split(",")) {
            String t = r.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
