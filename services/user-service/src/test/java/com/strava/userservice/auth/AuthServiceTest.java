package com.strava.userservice.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.ApiRequestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static com.strava.userservice.common.ErrorMessages.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokens;
    @Mock private UserEventPublisher events;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private StravaTokenExchanger stravaTokenExchanger;
    @Mock private StravaProperties stravaProperties;

    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("$hashed$")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void stubTokens() {
        given(tokens.issueTokens(any(), any(), any()))
                .willReturn(new JwtTokenProvider.TokenPair("access-token", "refresh-token", 3600));
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void returnsTokensAndUserOnValidCredentials() {
            stubTokens();
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "$hashed$")).willReturn(true);

            AuthenticationResponse response = authService.login(
                    new AuthenticationRequest("john@example.com", "password123"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.user().email()).isEqualTo("john@example.com");
            assertThat(response.user().fullName()).isEqualTo("John Doe");
        }

        @Test
        void rejectsUnknownEmailWith401() {
            given(userRepository.findByEmail("nope@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new AuthenticationRequest("nope@example.com", "pw")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_CREDENTIALS)
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void rejectsWrongPasswordWith401() {
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrong", "$hashed$")).willReturn(false);

            assertThatThrownBy(() -> authService.login(new AuthenticationRequest("john@example.com", "wrong")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_CREDENTIALS);
        }

        @Test
        void rejectsPasswordlessOauthAccountWith401() {
            user.setPassword(null);
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(new AuthenticationRequest("john@example.com", "pw")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void createsLocalUserAndPublishesRegisteredEvent() {
            stubTokens();
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("$hashed$");
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(7L);
                return u;
            });

            AuthenticationResponse response = authService.register(new RegistrationRequest(
                    "New", "User", "new@example.com", "password123", "password123"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
            assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
            assertThat(saved.getValue().isActive()).isTrue();
            assertThat(saved.getValue().getPassword()).isEqualTo("$hashed$");
            verify(events).publishUserRegistered(any(User.class));
            assertThat(response.user().id()).isEqualTo(7L);
        }

        @Test
        void rejectsDuplicateEmailWith400() {
            given(userRepository.existsByEmail("john@example.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(new RegistrationRequest(
                    "John", "Doe", "john@example.com", "password123", "password123")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_ALREADY_EXISTS)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void rejectsMismatchedConfirmPasswordWith400() {
            given(userRepository.existsByEmail("john@example.com")).willReturn(false);

            assertThatThrownBy(() -> authService.register(new RegistrationRequest(
                    "John", "Doe", "john@example.com", "password123", "different")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(PASSWORDS_DO_NOT_MATCH);
        }
    }

    @Nested
    @DisplayName("googleLogin")
    class GoogleLogin {

        @Test
        void createsNewGoogleUserFromTokenInfo() {
            stubTokens();
            given(googleTokenVerifier.verify("id-token")).willReturn(Map.of(
                    "email", "g@example.com",
                    "given_name", "Gee",
                    "family_name", "User",
                    "sub", "google-sub-1",
                    "picture", "http://pic"));
            given(userRepository.findByEmail("g@example.com")).willReturn(Optional.empty());
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(9L);
                return u;
            });

            AuthenticationResponse response = authService.googleLogin(new GoogleAuthRequest("id-token"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(saved.getValue().getProviderId()).isEqualTo("google-sub-1");
            assertThat(saved.getValue().getAvatar()).isEqualTo("http://pic");
            verify(events).publishUserRegistered(any(User.class));
            assertThat(response.user().email()).isEqualTo("g@example.com");
        }

        @Test
        void logsInExistingGoogleUserWithoutCreating() {
            stubTokens();
            user.setAuthProvider(AuthProvider.GOOGLE);
            given(googleTokenVerifier.verify("id-token"))
                    .willReturn(Map.of("email", "john@example.com"));
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));

            AuthenticationResponse response = authService.googleLogin(new GoogleAuthRequest("id-token"));

            verify(userRepository, never()).save(any());
            assertThat(response.user().id()).isEqualTo(1L);
        }

        @Test
        void rejectsEmailRegisteredUnderAnotherProviderWith409() {
            given(googleTokenVerifier.verify("id-token"))
                    .willReturn(Map.of("email", "john@example.com"));
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.googleLogin(new GoogleAuthRequest("id-token")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOOGLE_AUTH_EMAIL_MISMATCH)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void rejectsTokenInfoWithoutEmailWith400() {
            given(googleTokenVerifier.verify("id-token")).willReturn(Map.of("sub", "x"));

            assertThatThrownBy(() -> authService.googleLogin(new GoogleAuthRequest("id-token")))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GOOGLE_AUTH_ERROR);
        }
    }

    @Nested
    @DisplayName("stravaLogin")
    class StravaLogin {

        private final ObjectMapper json = new ObjectMapper();

        @Test
        void createsUserWithPlaceholderEmailOnFirstLogin() throws Exception {
            stubTokens();
            given(stravaProperties.getLoginRedirectUri()).willReturn("http://cb");
            given(stravaTokenExchanger.exchange("code-1", "http://cb")).willReturn(json.readTree("""
                    {"athlete": {"id": 12345, "firstname": "Athlete", "lastname": "One",
                                 "profile": "http://avatar", "city": "Warsaw", "country": "Poland"}}
                    """));
            given(userRepository.findByProviderIdAndAuthProvider("12345", AuthProvider.STRAVA))
                    .willReturn(Optional.empty());
            given(userRepository.existsByEmail("strava_12345@stride.local")).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(11L);
                return u;
            });

            AuthenticationResponse response = authService.stravaLogin("code-1");

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getEmail()).isEqualTo("strava_12345@stride.local");
            assertThat(saved.getValue().getAuthProvider()).isEqualTo(AuthProvider.STRAVA);
            assertThat(saved.getValue().getCity()).isEqualTo("Warsaw");
            verify(events).publishUserRegistered(any(User.class));
            assertThat(response.user().id()).isEqualTo(11L);
        }

        @Test
        void logsInExistingStravaUserByAthleteId() throws Exception {
            stubTokens();
            user.setAuthProvider(AuthProvider.STRAVA);
            user.setProviderId("12345");
            given(stravaProperties.getLoginRedirectUri()).willReturn("http://cb");
            given(stravaTokenExchanger.exchange(eq("code-1"), anyString()))
                    .willReturn(json.readTree("{\"athlete\": {\"id\": 12345}}"));
            given(userRepository.findByProviderIdAndAuthProvider("12345", AuthProvider.STRAVA))
                    .willReturn(Optional.of(user));

            AuthenticationResponse response = authService.stravaLogin("code-1");

            verify(userRepository, never()).save(any());
            assertThat(response.user().id()).isEqualTo(1L);
        }

        @Test
        void rejectsTokenResponseWithoutAthleteIdWith502() throws Exception {
            given(stravaProperties.getLoginRedirectUri()).willReturn("http://cb");
            given(stravaTokenExchanger.exchange(eq("code-1"), anyString()))
                    .willReturn(json.readTree("{\"access_token\": \"x\"}"));

            assertThatThrownBy(() -> authService.stravaLogin("code-1"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(STRAVA_AUTH_ERROR)
                    .extracting("status").isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        void rejectsPlaceholderEmailConflictWith409() throws Exception {
            given(stravaProperties.getLoginRedirectUri()).willReturn("http://cb");
            given(stravaTokenExchanger.exchange(eq("code-1"), anyString()))
                    .willReturn(json.readTree("{\"athlete\": {\"id\": 12345}}"));
            given(userRepository.findByProviderIdAndAuthProvider("12345", AuthProvider.STRAVA))
                    .willReturn(Optional.empty());
            given(userRepository.existsByEmail("strava_12345@stride.local")).willReturn(true);

            assertThatThrownBy(() -> authService.stravaLogin("code-1"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(STRAVA_PLACEHOLDER_EMAIL_CONFLICT)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        void issuesFreshTokenPairForValidRefreshToken() {
            stubTokens();
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.get("type", String.class)).willReturn("refresh");
            given(claims.get("userId", Number.class)).willReturn(1L);
            given(tokens.parse("valid-refresh")).willReturn(claims);
            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            AuthenticationResponse response = authService.refreshToken("valid-refresh");

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.user().id()).isEqualTo(1L);
        }

        @Test
        void rejectsMissingTokenWith401() {
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_JWT_TOKEN)
                    .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void rejectsUnparseableTokenWith401() {
            given(tokens.parse("garbage")).willThrow(new MalformedJwtException("bad"));

            assertThatThrownBy(() -> authService.refreshToken("garbage"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_JWT_TOKEN);
        }

        @Test
        void rejectsAccessTokenUsedAsRefreshWith401() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.get("type", String.class)).willReturn(null);
            given(tokens.parse("access-not-refresh")).willReturn(claims);

            assertThatThrownBy(() -> authService.refreshToken("access-not-refresh"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_JWT_TOKEN);
        }

        @Test
        void rejectsDeletedUserWith404() {
            Claims claims = org.mockito.Mockito.mock(Claims.class);
            given(claims.get("type", String.class)).willReturn("refresh");
            given(claims.get("userId", Number.class)).willReturn(99L);
            given(tokens.parse("valid-refresh")).willReturn(claims);
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("valid-refresh"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(USER_NOT_FOUND)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("forgot / reset password")
    class PasswordReset {

        @Test
        void forgotPasswordStoresCodeWithOneHourExpiry() {
            given(userRepository.findByEmail("john@example.com")).willReturn(Optional.of(user));

            authService.forgotPassword("john@example.com");

            assertThat(user.getPasswordResetCode()).isNotBlank();
            assertThat(user.getPasswordResetCodeExpiry())
                    .isAfter(LocalDateTime.now().plusMinutes(55))
                    .isBefore(LocalDateTime.now().plusMinutes(65));
            verify(userRepository).save(user);
        }

        @Test
        void forgotPasswordRevealsUnknownEmailWith404() {
            // Parity quirk with the monolith: unknown emails 404 instead of a neutral 200.
            given(userRepository.findByEmail("nope@example.com")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword("nope@example.com"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(EMAIL_NOT_FOUND)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void resetPasswordEncodesAndClearsCode() {
            user.setPasswordResetCode("code-1");
            user.setPasswordResetCodeExpiry(LocalDateTime.now().plusMinutes(30));
            given(userRepository.findByPasswordResetCode("code-1")).willReturn(Optional.of(user));
            given(passwordEncoder.encode("newpassword")).willReturn("$new$");

            authService.resetPassword("code-1", "newpassword");

            assertThat(user.getPassword()).isEqualTo("$new$");
            assertThat(user.getPasswordResetCode()).isNull();
            assertThat(user.getPasswordResetCodeExpiry()).isNull();
        }

        @Test
        void rejectsUnknownResetCodeWith400() {
            given(userRepository.findByPasswordResetCode("bad")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("bad", "newpassword"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(INVALID_PASSWORD_CODE);
        }

        @Test
        void rejectsExpiredResetCodeWith400() {
            user.setPasswordResetCode("code-1");
            user.setPasswordResetCodeExpiry(LocalDateTime.now().minusMinutes(1));
            given(userRepository.findByPasswordResetCode("code-1")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword("code-1", "newpassword"))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(PASSWORD_CODE_EXPIRED);
        }
    }
}
