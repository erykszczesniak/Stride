package com.strava.userservice.auth;

import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contract tests for the "Continue with Strava" login flow under {@code /api/v1/auth/strava}. */
@WebMvcTest(StravaAuthController.class)
@Import(SecurityConfig.class)
@DisplayName("StravaAuthController")
class StravaAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private StravaProperties stravaProperties;

    @MockitoBean
    private StravaStateSigner stateSigner;

    private AuthenticationResponse authResponse() {
        User user = User.builder()
                .id(7L).firstName("Athlete").lastName("One")
                .email("strava_12345@stride.local")
                .authProvider(AuthProvider.STRAVA).role(Role.USER).active(true)
                .build();
        return new AuthenticationResponse("access-token", "refresh-token", UserResponse.from(user));
    }

    @BeforeEach
    void setUp() {
        given(stravaProperties.getLoginFrontendErrorRedirect())
                .willReturn("http://localhost:3000/login");
    }

    @Nested
    @DisplayName("GET /api/v1/auth/strava/authorize")
    class Authorize {

        @Test
        void returnsAuthorizeUrlAsJsonByDefault() throws Exception {
            given(stravaProperties.isConfigured()).willReturn(true);
            given(stravaProperties.getClientId()).willReturn("client-1");
            given(stravaProperties.getLoginRedirectUri())
                    .willReturn("http://localhost:8080/api/v1/auth/strava/callback");
            given(stateSigner.signLoginState()).willReturn("signed-state");

            mockMvc.perform(get("/api/v1/auth/strava/authorize"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.startsWith("https://www.strava.com/oauth/authorize"),
                            org.hamcrest.Matchers.containsString("client_id=client-1"),
                            org.hamcrest.Matchers.containsString("scope=read,profile:read_all"),
                            org.hamcrest.Matchers.containsString("state=signed-state"))));
        }

        @Test
        void redirectsWhenRequested() throws Exception {
            given(stravaProperties.isConfigured()).willReturn(true);
            given(stravaProperties.getClientId()).willReturn("client-1");
            given(stravaProperties.getLoginRedirectUri()).willReturn("http://cb");
            given(stateSigner.signLoginState()).willReturn("signed-state");

            mockMvc.perform(get("/api/v1/auth/strava/authorize").param("redirect", "true"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.startsWith("https://www.strava.com/oauth/authorize")));
        }

        @Test
        void unconfiguredIs503() throws Exception {
            given(stravaProperties.isConfigured()).willReturn(false);

            mockMvc.perform(get("/api/v1/auth/strava/authorize"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error")
                            .value("Strava login is not configured on this server"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/strava/callback")
    class Callback {

        @Test
        void successfulCallbackRedirectsToFrontendWithTokens() throws Exception {
            given(stravaProperties.getLoginFrontendRedirect())
                    .willReturn("http://localhost:3000/auth/strava/callback");
            given(authService.stravaLogin("code-1")).willReturn(authResponse());

            mockMvc.perform(get("/api/v1/auth/strava/callback")
                            .param("code", "code-1").param("state", "signed-state"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("http://localhost:3000/auth/strava/callback"
                            + "?token=access-token&refreshToken=refresh-token&userId=7"));
        }

        @Test
        void deniedByUserRedirectsWithStravaErrorDenied() throws Exception {
            mockMvc.perform(get("/api/v1/auth/strava/callback").param("error", "access_denied"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("http://localhost:3000/login?stravaError=denied"));
        }

        @Test
        void invalidStateRedirectsWithCallbackFailed() throws Exception {
            willThrow(new ApiRequestException("Invalid Strava OAuth state signature", HttpStatus.BAD_REQUEST))
                    .given(stateSigner).verifyLoginState("tampered");

            mockMvc.perform(get("/api/v1/auth/strava/callback")
                            .param("code", "code-1").param("state", "tampered"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("http://localhost:3000/login?stravaError=callback-failed"));
        }

        @Test
        void exchangeFailureRedirectsWithCallbackFailed() throws Exception {
            given(authService.stravaLogin("code-1"))
                    .willThrow(new ApiRequestException("Strava exchange code failed", HttpStatus.BAD_GATEWAY));

            mockMvc.perform(get("/api/v1/auth/strava/callback")
                            .param("code", "code-1").param("state", "signed-state"))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl("http://localhost:3000/login?stravaError=callback-failed"));
        }
    }
}
