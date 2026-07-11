package com.strava.backend.security.pipeline;

import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.pipeline.PipelineTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates {@code /pipeline/v1/**} requests with a personal API token
 * ({@code Authorization: Bearer tri_...}). Mirrors {@link
 * com.strava.backend.security.jwt.JwtAuthenticationFilter} but resolves the
 * principal through the hashed pipeline-token store instead of JWT claims.
 *
 * Deliberately NOT a {@code @Component}: it is instantiated directly inside
 * the pipeline security chain so it never gets auto-registered as a global
 * servlet filter (and never leaks into @WebMvcTest slices, which scan Filter
 * beans and would then require a PipelineTokenService mock in every test).
 */
@RequiredArgsConstructor
public class PipelineTokenFilter extends OncePerRequestFilter {

    private final PipelineTokenService pipelineTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getBearerToken(request);
            if (StringUtils.hasText(token)) {
                pipelineTokenService.authenticate(token).ifPresent(user -> {
                    UserPrincipal principal = UserPrincipal.create(user);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    principal.getAuthorities()
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        } catch (Exception ex) {
            logger.error("Could not authenticate pipeline token", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
