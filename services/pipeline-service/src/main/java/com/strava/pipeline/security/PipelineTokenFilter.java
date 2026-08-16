package com.strava.pipeline.security;

import com.strava.pipeline.token.PipelineTokenService;
import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import com.strava.shared.security.JwtClaims;
import com.strava.shared.security.JwtParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Authenticates {@code /pipeline/v1/**} requests, ported from main. The
 * gateway forwards this surface unauthenticated (tri_ tokens are not JWTs),
 * so this service checks the credential itself. Two bearer credentials are
 * accepted:
 *
 * <ul>
 *   <li>a personal API token ({@code tri_...}) resolved through the hashed
 *       pipeline-token store — the long-lived, read-only-client credential;</li>
 *   <li>a regular Stride JWT (validated with the shared {@code jwt.secret})
 *       — so a session that logged in via {@code /api/v1/auth/login} (web or
 *       mobile) can read pipeline analytics without minting a separate
 *       token.</li>
 * </ul>
 *
 * On success the SecurityContext holds a {@link HeaderAuthenticationToken}
 * whose principal is the Long user id — the same principal shape the
 * gateway-header chain produces, so {@code @AuthenticationPrincipal Long}
 * works identically on both surfaces.
 *
 * Deliberately NOT a {@code @Component}: it is instantiated directly inside
 * the pipeline security chain so it never gets auto-registered as a global
 * servlet filter (and never leaks into @WebMvcTest slices, which scan Filter
 * beans and would then require mocks in every test).
 */
@RequiredArgsConstructor
public class PipelineTokenFilter extends OncePerRequestFilter {

    private final PipelineTokenService pipelineTokenService;
    private final JwtParser jwtParser;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getBearerToken(request);
        if (StringUtils.hasText(token)) {
            if (token.startsWith(PipelineTokenService.TOKEN_PREFIX)) {
                authenticateWithPersonalToken(token, request);
            } else {
                authenticateWithJwt(token, request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateWithPersonalToken(String token, HttpServletRequest request) {
        try {
            pipelineTokenService.authenticate(token)
                    .ifPresent(userId -> setAuthentication(userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")), request));
        } catch (Exception ex) {
            logger.error("Could not authenticate pipeline token", ex);
        }
    }

    private void authenticateWithJwt(String jwt, HttpServletRequest request) {
        try {
            JwtClaims claims = jwtParser.parse(jwt);
            setAuthentication(claims.userId(), claims.email(), toAuthorities(claims.roles()), request);
        } catch (Exception ex) {
            // An invalid or expired JWT is just an unauthenticated request here
            // (the entry point turns it into a 401), not a server-side error.
            logger.debug("Pipeline JWT authentication failed: " + ex.getMessage());
        }
    }

    private void setAuthentication(Long userId, String email,
                                   List<GrantedAuthority> authorities, HttpServletRequest request) {
        HeaderAuthenticationToken authentication =
                new HeaderAuthenticationToken(userId, email, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static List<GrantedAuthority> toAuthorities(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    private String getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
