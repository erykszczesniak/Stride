package com.strava.backend.security.pipeline;

import com.strava.backend.domain.User;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.JwtAuthenticationException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.pipeline.PipelineTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineTokenFilter")
class PipelineTokenFilterTest {

    @Mock
    private PipelineTokenService pipelineTokenService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomUserDetailsService userDetailsService;

    private PipelineTokenFilter filter;

    private final User user = User.builder()
            .id(7L)
            .email("user@test.dev")
            .role(Role.USER)
            .build();

    @BeforeEach
    void setUp() {
        filter = new PipelineTokenFilter(pipelineTokenService, jwtProvider, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pipeline/v1/dashboard");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    private void runFilter(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    @DisplayName("authenticates a tri_ personal token via the token store")
    void personalTokenAuthenticates() throws Exception {
        given(pipelineTokenService.authenticate("tri_valid")).willReturn(Optional.of(user));

        runFilter(requestWithBearer("tri_valid"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((UserPrincipal) auth.getPrincipal()).getId()).isEqualTo(7L);
        verifyNoInteractions(jwtProvider);
    }

    @Test
    @DisplayName("authenticates a regular Stride JWT so logged-in sessions can read the pipeline")
    void jwtAuthenticates() throws Exception {
        given(jwtProvider.validateToken("eyJ.jwt")).willReturn(true);
        given(jwtProvider.getEmailFromToken("eyJ.jwt")).willReturn("user@test.dev");
        given(userDetailsService.loadUserByUsername("user@test.dev")).willReturn(UserPrincipal.create(user));

        runFilter(requestWithBearer("eyJ.jwt"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((UserPrincipal) auth.getPrincipal()).getId()).isEqualTo(7L);
        verifyNoInteractions(pipelineTokenService);
    }

    @Test
    @DisplayName("an invalid JWT leaves the request unauthenticated without throwing")
    void invalidJwtStaysUnauthenticated() throws Exception {
        given(jwtProvider.validateToken("garbage"))
                .willThrow(new JwtAuthenticationException("Invalid token", org.springframework.http.HttpStatus.UNAUTHORIZED));

        runFilter(requestWithBearer("garbage"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an unknown tri_ token leaves the request unauthenticated")
    void unknownPersonalTokenStaysUnauthenticated() throws Exception {
        given(pipelineTokenService.authenticate("tri_unknown")).willReturn(Optional.empty());

        runFilter(requestWithBearer("tri_unknown"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("no Authorization header touches neither credential path")
    void missingHeaderIsIgnored() throws Exception {
        runFilter(requestWithBearer(null));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(pipelineTokenService, jwtProvider, userDetailsService);
    }
}
