package com.strava.backend.controller;

import com.strava.backend.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaseController Unit Tests")
class BaseControllerTest {

    private final BaseController baseController = new BaseController() {};

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getAuthenticatedUserId Tests")
    class GetAuthenticatedUserIdTests {

        @Test
        @DisplayName("Should return userId when UserPrincipal is set")
        void shouldReturnUserIdWhenUserPrincipalIsSet() {
            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .id(42L)
                    .email("test@example.com")
                    .password("password")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                    .build();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Long userId = baseController.getAuthenticatedUserId();

            assertThat(userId).isEqualTo(42L);
        }

        @Test
        @DisplayName("Should return null when no authentication")
        void shouldReturnNullWhenNoAuthentication() {
            SecurityContextHolder.clearContext();

            Long userId = baseController.getAuthenticatedUserId();

            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("Should return null when principal is not UserPrincipal")
        void shouldReturnNullWhenPrincipalIsNotUserPrincipal() {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Long userId = baseController.getAuthenticatedUserId();

            assertThat(userId).isNull();
        }
    }
}
