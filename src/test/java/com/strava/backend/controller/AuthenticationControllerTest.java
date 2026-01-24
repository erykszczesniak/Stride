package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.RegistrationRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.exception.EmailException;
import com.strava.backend.exception.PasswordException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static com.strava.backend.constants.ErrorMessage.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthenticationController Unit Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private AuthenticationResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("john@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");

        authResponse = AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(userResponse)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            given(authenticationService.login(any(AuthenticationRequest.class))).willReturn(authResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.user.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return bad request when email is empty")
        void shouldReturnBadRequestWhenEmailEmpty() throws Exception {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("");
            request.setPassword("password123");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when password is empty")
        void shouldReturnBadRequestWhenPasswordEmpty() throws Exception {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("john@example.com");
            request.setPassword("");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register successfully")
        void shouldRegisterSuccessfully() throws Exception {
            // Given
            var request = new RegistrationRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("john@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");

            given(authenticationService.register(any(RegistrationRequest.class))).willReturn(authResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.user.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return bad request when email already exists")
        void shouldReturnBadRequestWhenEmailExists() throws Exception {
            // Given
            var request = new RegistrationRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");

            given(authenticationService.register(any(RegistrationRequest.class)))
                    .willThrow(new EmailException(USER_ALREADY_EXISTS));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when passwords do not match")
        void shouldReturnBadRequestWhenPasswordsDoNotMatch() throws Exception {
            // Given
            var request = new RegistrationRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("john@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("different");

            given(authenticationService.register(any(RegistrationRequest.class)))
                    .willThrow(new PasswordException(PASSWORDS_DO_NOT_MATCH));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when first name is empty")
        void shouldReturnBadRequestWhenFirstNameEmpty() throws Exception {
            // Given
            var request = new RegistrationRequest();
            request.setFirstName("");
            request.setLastName("Doe");
            request.setEmail("john@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Given
            given(authenticationService.refreshToken("valid-refresh-token")).willReturn(authResponse);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh-token"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("Should return unauthorized when refresh token is invalid")
        void shouldReturnUnauthorizedWhenTokenInvalid() throws Exception {
            // Given
            given(authenticationService.refreshToken("invalid-token"))
                    .willThrow(new ApiRequestException(INVALID_JWT_TOKEN, HttpStatus.UNAUTHORIZED));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("refreshToken", "invalid-token"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should send password reset email successfully")
        void shouldSendPasswordResetEmailSuccessfully() throws Exception {
            // Given
            willDoNothing().given(authenticationService).forgotPassword("john@example.com");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "john@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset email sent"));
        }

        @Test
        @DisplayName("Should return not found when email does not exist")
        void shouldReturnNotFoundWhenEmailNotExists() throws Exception {
            // Given
            willThrow(new ApiRequestException(EMAIL_NOT_FOUND, HttpStatus.NOT_FOUND))
                    .given(authenticationService).forgotPassword("nonexistent@example.com");

            // When/Then
            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("email", "nonexistent@example.com"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() throws Exception {
            // Given
            willDoNothing().given(authenticationService).resetPassword(anyString(), anyString());

            // When/Then
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", "valid-code",
                                    "password", "newpassword123"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password reset successfully"));
        }

        @Test
        @DisplayName("Should return bad request when reset code is invalid")
        void shouldReturnBadRequestWhenCodeInvalid() throws Exception {
            // Given
            willThrow(new ApiRequestException(INVALID_PASSWORD_CODE, HttpStatus.BAD_REQUEST))
                    .given(authenticationService).resetPassword(anyString(), anyString());

            // When/Then
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", "invalid-code",
                                    "password", "newpassword123"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return bad request when reset code is expired")
        void shouldReturnBadRequestWhenCodeExpired() throws Exception {
            // Given
            willThrow(new ApiRequestException(PASSWORD_CODE_EXPIRED, HttpStatus.BAD_REQUEST))
                    .given(authenticationService).resetPassword(anyString(), anyString());

            // When/Then
            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "code", "expired-code",
                                    "password", "newpassword123"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
