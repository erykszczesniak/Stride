package com.strava.shared.security;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HeaderAuthenticationFilter")
class HeaderAuthenticationFilterTest {

    private final HeaderAuthenticationFilter filter = new HeaderAuthenticationFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest identityRequest(DispatcherType dispatcherType) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.setDispatcherType(dispatcherType);
        request.addHeader(UserContext.HEADER_USER_ID, "42");
        request.addHeader(UserContext.HEADER_USER_EMAIL, "john@example.com");
        request.addHeader(UserContext.HEADER_USER_ROLES, "USER,ADMIN");
        return request;
    }

    @Test
    @DisplayName("authenticates from the gateway headers on a normal request")
    void authenticatesOnRequestDispatch() throws Exception {
        filter.doFilter(identityRequest(DispatcherType.REQUEST), new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(HeaderAuthenticationFilter.HeaderAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isEqualTo(42L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("re-establishes the identity on the ERROR dispatch, so /error is not an anonymous 403")
    void authenticatesOnErrorDispatch() throws Exception {
        assertThat(filter.shouldNotFilterErrorDispatch()).isFalse();

        filter.doFilter(identityRequest(DispatcherType.ERROR), new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(42L);
    }

    @Test
    @DisplayName("leaves the request anonymous when the identity header is missing or malformed")
    void anonymousWithoutValidHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader(UserContext.HEADER_USER_ID, "not-a-number");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
