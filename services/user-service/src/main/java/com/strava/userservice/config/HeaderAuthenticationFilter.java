package com.strava.userservice.config;

import com.strava.shared.security.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Trusts identity headers (X-User-Id / X-User-Email / X-User-Roles) propagated
 * by the API Gateway after it validated the JWT. Translates them into a
 * Spring Security Authentication so downstream code can use
 * `@AuthenticationPrincipal Long userId` and `@PreAuthorize`.
 *
 * NOTE: this is safe ONLY because the gateway strips spoofed copies of these
 * headers from inbound external requests (see JwtAuthenticationFilter in
 * the api-gateway module). In K8s, additionally restrict ingress so the
 * service is only reachable from the gateway pod.
 */
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String idHeader = request.getHeader(UserContext.HEADER_USER_ID);
        if (idHeader != null && !idHeader.isBlank()) {
            try {
                Long userId = Long.parseLong(idHeader);
                String email = request.getHeader(UserContext.HEADER_USER_EMAIL);
                Collection<? extends GrantedAuthority> authorities = parseAuthorities(
                        request.getHeader(UserContext.HEADER_USER_ROLES));
                SecurityContextHolder.getContext().setAuthentication(
                        new HeaderAuthenticationToken(userId, email, authorities));
            } catch (NumberFormatException ignored) {
                // malformed header — fall through unauthenticated
            }
        }
        chain.doFilter(request, response);
    }

    private static Collection<? extends GrantedAuthority> parseAuthorities(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    static class HeaderAuthenticationToken extends AbstractAuthenticationToken {
        private final Long userId;
        private final String email;

        HeaderAuthenticationToken(Long userId, String email,
                                  Collection<? extends GrantedAuthority> authorities) {
            super(authorities);
            this.userId = userId;
            this.email = email;
            setAuthenticated(true);
        }

        @Override public Object getPrincipal() { return userId; }
        @Override public Object getCredentials() { return ""; }
        public String getEmail() { return email; }
    }
}
