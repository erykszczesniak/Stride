package com.strava.userservice.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.user.UserResponse;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.strava.userservice.common.ErrorMessages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@code /api/v1/auth}, ported from the monolith's
 * {@code AuthenticationControllerTest}. {@link SecurityConfig} is imported so the
 * permitAll rule for {@code /api/v1/auth/**} is exercised (no identity headers sent).
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private AuthenticationResponse authResponse() {
        User user = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john@example.com")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
        return new AuthenticationResponse("access-token", "refresh-token", UserResponse.from(user));
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        void loginSucceedsWithValidCredentials() throws Exception {
            given(authService.login(any(AuthenticationRequest.class))).willReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "john@example.com", "password", "password123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.user.email").value("john@example.com"))
                    .andExpect(jsonPath("$.user.fullName").value("John Doe"));
        }

        @Test
        void emptyEmailIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "", "password", "password123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void emptyPasswordIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "john@example.com", "password", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").value(EMPTY_PASSWORD));
        }

        @Test
        void badCredentialsSurfaceMessageProperty() throws Exception {
            given(authService.login(any(AuthenticationRequest.class)))
                    .willThrow(new ApiRequestException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("email", "john@example.com", "password", "wrong"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(INVALID_CREDENTIALS));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        void registerReturns201WithAuthPayload() throws Exception {
            given(authService.register(any(RegistrationRequest.class))).willReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "John", "lastName", "Doe",
                                    "email", "john@example.com",
                                    "password", "password123", "confirmPassword", "password123"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.user.email").value("john@example.com"));
        }

        @Test
        void duplicateEmailIsBadRequest() throws Exception {
            given(authService.register(any(RegistrationRequest.class)))
                    .willThrow(new ApiRequestException(USER_ALREADY_EXISTS, HttpStatus.BAD_REQUEST));

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "John", "lastName", "Doe",
                                    "email", "existing@example.com",
                                    "password", "password123", "confirmPassword", "password123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(USER_ALREADY_EXISTS));
        }

        @Test
        void emptyFirstNameIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "", "lastName", "Doe",
                                    "email", "john@example.com",
                                    "password", "password123", "confirmPassword", "password123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.firstName").value(EMPTY_FIRST_NAME));
        }

        @Test
        void shortPasswordIsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "John", "lastName", "Doe",
                                    "email", "john@example.com",
                                    "password", "short", "confirmPassword", "short"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.password").value(PASSWORD_MIN_LENGTH));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/google")
    class Google {

        @Test
        void googleLoginReturnsAuthPayload() throws Exception {
            given(authService.googleLogin(any(GoogleAuthRequest.class))).willReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("idToken", "google-id-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }

        @Test
        void providerMismatchIsConflict() throws Exception {
            given(authService.googleLogin(any(GoogleAuthRequest.class)))
                    .willThrow(new ApiRequestException(GOOGLE_AUTH_EMAIL_MISMATCH, HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("idToken", "google-id-token"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(GOOGLE_AUTH_EMAIL_MISMATCH));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        void refreshReturnsNewTokenPair() throws Exception {
            given(authService.refreshToken("valid-refresh-token")).willReturn(authResponse());

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("refreshToken", "valid-refresh-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        void invalidTokenIsUnauthorized() throws Exception {
            given(authService.refreshToken("invalid-token"))
                    .willThrow(new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED));

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(INVALID_JWT_TOKEN));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password + /reset-password")
    class PasswordReset {

        @Test
        void forgotPasswordReturnsFixedMessage() throws Exception {
            willDoNothing().given(authService).forgotPassword("john@example.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "john@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent"));
        }

        @Test
        void unknownEmailIsNotFound() throws Exception {
            willThrow(new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND))
                    .given(authService).forgotPassword("nonexistent@example.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "nonexistent@example.com"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(EMAIL_NOT_FOUND));
        }

        @Test
        void resetPasswordReturnsFixedMessage() throws Exception {
            willDoNothing().given(authService).resetPassword(anyString(), anyString());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("code", "valid-code", "password", "newpassword123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"));
        }

        @Test
        void invalidResetCodeIsBadRequest() throws Exception {
            willThrow(new ApiRequestException(INVALID_PASSWORD_CODE, HttpStatus.BAD_REQUEST))
                    .given(authService).resetPassword(anyString(), anyString());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("code", "invalid-code", "password", "newpassword123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(INVALID_PASSWORD_CODE));
        }

        @Test
        void expiredResetCodeIsBadRequest() throws Exception {
            willThrow(new ApiRequestException(PASSWORD_CODE_EXPIRED, HttpStatus.BAD_REQUEST))
                    .given(authService).resetPassword(anyString(), anyString());

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("code", "expired-code", "password", "newpassword123"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(PASSWORD_CODE_EXPIRED));
        }
    }
}
