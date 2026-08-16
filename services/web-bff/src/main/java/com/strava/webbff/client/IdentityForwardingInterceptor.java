package com.strava.webbff.client;

import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import com.strava.shared.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Forwards the caller's identity headers (X-User-Id / X-User-Email / X-User-Roles)
 * on every outbound composition call, so downstream services see the BFF exactly
 * like gateway traffic and their {@code HeaderAuthenticationFilter} authenticates it.
 *
 * <p>Two sources, in order:
 * <ol>
 *   <li>the current servlet request ({@link RequestContextHolder}) — copies the
 *       inbound X-User-* headers verbatim (REST controllers run on the request
 *       thread, so this is the common path);</li>
 *   <li>the {@link SecurityContextHolder} — rebuilds the headers from the
 *       {@link HeaderAuthenticationToken}. This covers GraphQL data fetchers,
 *       where Spring for GraphQL propagates the security context via context
 *       snapshots even when execution hops threads, while the request
 *       attributes thread-local may not follow.</li>
 * </ol>
 */
public class IdentityForwardingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (!fromCurrentRequest(request)) {
            fromSecurityContext(request);
        }
        return execution.execute(request, body);
    }

    private boolean fromCurrentRequest(HttpRequest outbound) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return false;
        }
        HttpServletRequest inbound = attributes.getRequest();
        String userId = inbound.getHeader(UserContext.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            return false;
        }
        outbound.getHeaders().set(UserContext.HEADER_USER_ID, userId);
        copyIfPresent(inbound, outbound, UserContext.HEADER_USER_EMAIL);
        copyIfPresent(inbound, outbound, UserContext.HEADER_USER_ROLES);
        return true;
    }

    private void fromSecurityContext(HttpRequest outbound) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof HeaderAuthenticationToken token)) {
            return;
        }
        outbound.getHeaders().set(UserContext.HEADER_USER_ID, String.valueOf(token.getPrincipal()));
        if (token.getEmail() != null) {
            outbound.getHeaders().set(UserContext.HEADER_USER_EMAIL, token.getEmail());
        }
        // HeaderAuthenticationFilter prefixes roles with "ROLE_"; the wire format is without it.
        String roles = token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .collect(Collectors.joining(","));
        if (!roles.isBlank()) {
            outbound.getHeaders().set(UserContext.HEADER_USER_ROLES, roles);
        }
    }

    private static void copyIfPresent(HttpServletRequest inbound, HttpRequest outbound, String header) {
        String value = inbound.getHeader(header);
        if (value != null && !value.isBlank()) {
            outbound.getHeaders().set(header, value);
        }
    }
}
