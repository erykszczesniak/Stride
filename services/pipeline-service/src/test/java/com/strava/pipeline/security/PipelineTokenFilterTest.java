package com.strava.pipeline.security;

import com.strava.pipeline.token.PipelineTokenService;
import com.strava.shared.security.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Ported from main. The JWT leg exercises the real shared JwtParser with
 * tokens minted against a test secret instead of a mocked provider — same
 * outcomes, stronger coverage of the parsing path.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineTokenFilter")
class PipelineTokenFilterTest {

    private static final String SECRET = "test-secret-min-32-chars-used-only-inside-unit-tests";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock
    private PipelineTokenService pipelineTokenService;

    private PipelineTokenFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PipelineTokenFilter(pipelineTokenService, new JwtParser(SECRET));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static String jwt(long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("email", "user@test.dev")
                .claim("roles", List.of("USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(KEY)
                .compact();
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
        given(pipelineTokenService.authenticate("tri_valid")).willReturn(Optional.of(7L));

        runFilter(requestWithBearer("tri_valid"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("authenticates a regular Stride JWT so logged-in sessions can read the pipeline")
    void jwtAuthenticates() throws Exception {
        runFilter(requestWithBearer(jwt(7L)));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        verifyNoInteractions(pipelineTokenService);
    }

    @Test
    @DisplayName("an invalid JWT leaves the request unauthenticated without throwing")
    void invalidJwtStaysUnauthenticated() throws Exception {
        runFilter(requestWithBearer("garbage"));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(pipelineTokenService);
    }

    @Test
    @DisplayName("a JWT signed with a different secret is rejected")
    void foreignSignatureStaysUnauthenticated() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-of-at-least-32-chars-for-tests!".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .subject("7")
                .claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        runFilter(requestWithBearer(forged));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an expired JWT leaves the request unauthenticated")
    void expiredJwtStaysUnauthenticated() throws Exception {
        String expired = Jwts.builder()
                .subject("7")
                .claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(KEY)
                .compact();

        runFilter(requestWithBearer(expired));

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
        verifyNoInteractions(pipelineTokenService);
    }
}
