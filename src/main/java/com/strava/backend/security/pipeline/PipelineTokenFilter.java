package com.strava.backend.security.pipeline;

import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.pipeline.PipelineTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates {@code /pipeline/v1/**} requests. Two bearer credentials are
 * accepted:
 *
 * <ul>
 *   <li>a personal API token ({@code tri_...}) resolved through the hashed
 *       pipeline-token store — the long-lived, read-only-client credential;</li>
 *   <li>a regular Stride JWT — so a session that logged in via
 *       {@code /api/v1/auth/login} (web or mobile) can read pipeline analytics
 *       without minting a separate token.</li>
 * </ul>
 *
 * Deliberately NOT a {@code @Component}: it is instantiated directly inside
 * the pipeline security chain so it never gets auto-registered as a global
 * servlet filter (and never leaks into @WebMvcTest slices, which scan Filter
 * beans and would then require mocks in every test).
 */
@RequiredArgsConstructor
public class PipelineTokenFilter extends OncePerRequestFilter {

    private final PipelineTokenService pipelineTokenService;
    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

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
                    .ifPresent(user -> setAuthentication(UserPrincipal.create(user), request));
        } catch (Exception ex) {
            logger.error("Could not authenticate pipeline token", ex);
        }
    }

    private void authenticateWithJwt(String jwt, HttpServletRequest request) {
        try {
            if (jwtProvider.validateToken(jwt)) {
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(jwtProvider.getEmailFromToken(jwt));
                setAuthentication(userDetails, request);
            }
        } catch (Exception ex) {
            // An invalid or expired JWT is just an unauthenticated request here
            // (the entry point turns it into a 401), not a server-side error.
            logger.debug("Pipeline JWT authentication failed: " + ex.getMessage());
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
