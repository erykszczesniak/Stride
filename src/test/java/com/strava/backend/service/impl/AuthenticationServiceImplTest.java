package com.strava.backend.service.impl;

import com.strava.backend.domain.User;
import com.strava.backend.dto.auth.AuthenticationRequest;
import com.strava.backend.dto.auth.AuthenticationResponse;
import com.strava.backend.dto.auth.RegistrationRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.exception.EmailException;
import com.strava.backend.exception.PasswordException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.strava.backend.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationServiceImpl Unit Tests")
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CommonMapper commonMapper;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private User testUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");
        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("john@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            var userPrincipal = mock(UserPrincipal.class);
            var authentication = mock(Authentication.class);

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userPrincipal.getId()).willReturn(1L);
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(jwtProvider.generateToken(1L, "john@example.com")).willReturn("access-token");
            given(jwtProvider.generateRefreshToken(1L, "john@example.com")).willReturn("refresh-token");
            given(commonMapper.convertToResponse(testUser, UserResponse.class)).willReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");

            then(authenticationManager).should().authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found after authentication")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("john@example.com");
            request.setPassword("password123");

            var userPrincipal = mock(UserPrincipal.class);
            var authentication = mock(Authentication.class);

            given(authentication.getPrincipal()).willReturn(userPrincipal);
            given(userPrincipal.getId()).willReturn(999L);
            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw exception when credentials are invalid")
        void shouldThrowExceptionWhenCredentialsInvalid() {
            // Given
            var request = new AuthenticationRequest();
            request.setEmail("john@example.com");
            request.setPassword("wrongpassword");

            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authenticationService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterSuccessfully() {
            // Given
            var request = new RegistrationRequest();
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setEmail("newuser@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");

            given(userRepository.existsByEmail("newuser@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encoded-password");
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });
            given(jwtProvider.generateToken(anyLong(), anyString())).willReturn("access-token");
            given(jwtProvider.generateRefreshToken(anyLong(), anyString())).willReturn("refresh-token");
            given(commonMapper.convertToResponse(any(User.class), eq(UserResponse.class))).willReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.register(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            var request = new RegistrationRequest();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("password123");

            given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

            // When/Then
            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(EmailException.class)
                    .satisfies(ex -> assertThat(((EmailException) ex).getEmailError()).isEqualTo(USER_ALREADY_EXISTS));

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when passwords do not match")
        void shouldThrowExceptionWhenPasswordsDoNotMatch() {
            // Given
            var request = new RegistrationRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("password123");
            request.setConfirmPassword("differentpassword");

            given(userRepository.existsByEmail("newuser@example.com")).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> authenticationService.register(request))
                    .isInstanceOf(PasswordException.class)
                    .satisfies(ex -> assertThat(((PasswordException) ex).getPasswordError()).isEqualTo(PASSWORDS_DO_NOT_MATCH));

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("refreshToken Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            String refreshToken = "valid-refresh-token";

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getEmailFromToken(refreshToken)).willReturn("john@example.com");
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(testUser));
            given(jwtProvider.generateToken(1L, "john@example.com")).willReturn("new-access-token");
            given(jwtProvider.generateRefreshToken(1L, "john@example.com")).willReturn("new-refresh-token");
            given(commonMapper.convertToResponse(testUser, UserResponse.class)).willReturn(userResponse);

            // When
            AuthenticationResponse response = authenticationService.refreshToken(refreshToken);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("Should throw exception when refresh token is invalid")
        void shouldThrowExceptionWhenTokenInvalid() {
            // Given
            String invalidToken = "invalid-token";

            given(jwtProvider.validateToken(invalidToken)).willReturn(false);

            // When/Then
            assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_JWT_TOKEN)
                    .extracting("status")
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should throw exception when user not found during token refresh")
        void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
            // Given
            String refreshToken = "valid-token";

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getEmailFromToken(refreshToken)).willReturn("nonexistent@example.com");
            given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authenticationService.refreshToken(refreshToken))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("forgotPassword Tests")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should generate reset code for valid email")
        void shouldGenerateResetCodeForValidEmail() {
            // Given
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            authenticationService.forgotPassword("john@example.com");

            // Then
            then(userRepository).should().save(any(User.class));
            assertThat(testUser.getPasswordResetCode()).isNotNull();
            assertThat(testUser.getPasswordResetCodeExpiry()).isNotNull();
            assertThat(testUser.getPasswordResetCodeExpiry()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should throw exception when email not found")
        void shouldThrowExceptionWhenEmailNotFound() {
            // Given
            given(userRepository.findByEmail("nonexistent@example.com")).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authenticationService.forgotPassword("nonexistent@example.com"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(EMAIL_NOT_FOUND);

            then(userRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("resetPassword Tests")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            // Given
            String resetCode = "valid-reset-code";
            testUser.setPasswordResetCode(resetCode);
            testUser.setPasswordResetCodeExpiry(LocalDateTime.now().plusHours(1));

            given(userRepository.findByPasswordResetCode(resetCode)).willReturn(Optional.of(testUser));
            given(passwordEncoder.encode("newpassword")).willReturn("encoded-new-password");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            authenticationService.resetPassword(resetCode, "newpassword");

            // Then
            then(userRepository).should().save(any(User.class));
            assertThat(testUser.getPasswordResetCode()).isNull();
            assertThat(testUser.getPasswordResetCodeExpiry()).isNull();
        }

        @Test
        @DisplayName("Should throw exception when reset code is invalid")
        void shouldThrowExceptionWhenResetCodeInvalid() {
            // Given
            given(userRepository.findByPasswordResetCode("invalid-code")).willReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authenticationService.resetPassword("invalid-code", "newpassword"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_PASSWORD_CODE);
        }

        @Test
        @DisplayName("Should throw exception when reset code is expired")
        void shouldThrowExceptionWhenResetCodeExpired() {
            // Given
            String resetCode = "expired-code";
            testUser.setPasswordResetCode(resetCode);
            testUser.setPasswordResetCodeExpiry(LocalDateTime.now().minusHours(1));

            given(userRepository.findByPasswordResetCode(resetCode)).willReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> authenticationService.resetPassword(resetCode, "newpassword"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(PASSWORD_CODE_EXPIRED);

            then(userRepository).should(never()).save(any());
        }
    }
}
