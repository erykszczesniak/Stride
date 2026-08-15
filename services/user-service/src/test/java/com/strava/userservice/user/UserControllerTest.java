package com.strava.userservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.exception.ApiRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.strava.userservice.common.ErrorMessages.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for {@code /api/v1/users}, ported from the monolith's
 * {@code UserControllerTest}. {@link SecurityConfig} is imported so authentication runs
 * through the real {@code HeaderAuthenticationFilter} — identity is supplied via the
 * {@code X-User-*} headers the gateway would inject.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    private static final String USER_ID = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private User user(long id, String firstName, String lastName) {
        return User.builder()
                .id(id).firstName(firstName).lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }

    private UserResponse userResponse() {
        return UserResponse.from(user(1L, "John", "Doe"), 3, 2, 5);
    }

    @Nested
    @DisplayName("authentication")
    class Authentication {

        @Test
        void requestWithoutIdentityHeadersIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me and /{userId}")
    class GetUser {

        @Test
        void meReturnsCurrentUserWithCounters() throws Exception {
            given(userService.getUserResponse(1L)).willReturn(userResponse());

            mockMvc.perform(get("/api/v1/users/me").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.followersCount").value(3))
                    .andExpect(jsonPath("$.followingCount").value(2))
                    .andExpect(jsonPath("$.activitiesCount").value(5))
                    // Boolean-object fields keep their monolith JSON names.
                    .andExpect(jsonPath("$.isPrivate").value(false))
                    .andExpect(jsonPath("$.hideFromSearch").value(false))
                    .andExpect(jsonPath("$.emailNotifications").value(true));
        }

        @Test
        void byIdReturnsUser() throws Exception {
            given(userService.getUserResponse(2L)).willReturn(
                    UserResponse.from(user(2L, "Jane", "Smith")));

            mockMvc.perform(get("/api/v1/users/2").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.firstName").value("Jane"));
        }

        @Test
        void unknownUserIs404WithMessage() throws Exception {
            given(userService.getUserResponse(99L))
                    .willThrow(new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/users/99").header(USER_ID, "1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users (paged list)")
    class ListUsers {

        @Test
        void returnsPlainListBodyWithPagingHeaders() throws Exception {
            given(userService.getAllUsers(any(Pageable.class))).willReturn(new PageImpl<>(
                    List.of(user(1L, "John", "Doe"), user(2L, "Jane", "Smith")),
                    PageRequest.of(0, 20), 2));

            mockMvc.perform(get("/api/v1/users").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("page-total-count", "1"))
                    .andExpect(header().string("page-total-elements", "2"))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].firstName").value("Jane"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/search")
    class Search {

        @Test
        void returnsMatchesAsPlainList() throws Exception {
            given(userService.searchUsers(eq("john"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(user(1L, "John", "Doe"))));

            mockMvc.perform(get("/api/v1/users/search").param("query", "john").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }

        @Test
        void fallsBackToSqlWhenElasticsearchFails() throws Exception {
            given(userService.searchUsers(eq("john"), any(Pageable.class)))
                    .willThrow(new IllegalStateException("ES down"));
            given(userService.searchUsersFallback(eq("john"), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(user(1L, "John", "Doe"))));

            mockMvc.perform(get("/api/v1/users/search").param("query", "john").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateMe {

        @Test
        void updatesProfileAndReturnsFreshUser() throws Exception {
            given(userService.getUserResponse(1L)).willReturn(userResponse());

            mockMvc.perform(put("/api/v1/users/me")
                            .header(USER_ID, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "John", "lastName", "Doe", "bio", "hi"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));

            verify(userService).updateUser(eq(1L), any(UpdateUserRequest.class));
        }

        @Test
        void blankFirstNameIsBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/users/me")
                            .header(USER_ID, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "firstName", "", "lastName", "Doe"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.fieldErrors.firstName").value("First name cannot be empty"));
        }
    }

    @Nested
    @DisplayName("follow endpoints")
    class Follow {

        @Test
        void followReturnsMessage() throws Exception {
            mockMvc.perform(post("/api/v1/users/2/follow").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User followed"));

            verify(userService).followUser(1L, 2L);
        }

        @Test
        void unfollowReturnsMessage() throws Exception {
            mockMvc.perform(delete("/api/v1/users/2/follow").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unfollowed"));

            verify(userService).unfollowUser(1L, 2L);
        }

        @Test
        void followSelfSurfaces400Message() throws Exception {
            willThrow(new ApiRequestException("Cannot follow yourself", HttpStatus.BAD_REQUEST))
                    .given(userService).followUser(1L, 1L);

            mockMvc.perform(post("/api/v1/users/1/follow").header(USER_ID, "1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot follow yourself"));
        }

        @Test
        void followersReturnsBaseUserCards() throws Exception {
            given(userService.getFollowers(1L)).willReturn(List.of(user(2L, "Jane", "Smith")));

            mockMvc.perform(get("/api/v1/users/1/followers").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(2))
                    .andExpect(jsonPath("$[0].fullName").value("Jane Smith"))
                    // BaseUserResponse must not leak the email.
                    .andExpect(jsonPath("$[0].email").doesNotExist());
        }

        @Test
        void followingReturnsBaseUserCards() throws Exception {
            given(userService.getFollowing(1L)).willReturn(List.of(user(2L, "Jane", "Smith")));

            mockMvc.perform(get("/api/v1/users/1/following").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].firstName").value("Jane"));
        }

        @Test
        void suggestedUsesDefaultLimitOf5() throws Exception {
            given(userService.getSuggestedUsers(1L, 5)).willReturn(List.of(user(2L, "Jane", "Smith")));

            mockMvc.perform(get("/api/v1/users/suggested").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/profile")
    class Profile {

        @Test
        void serializesMonolithCompatibleBooleanKeys() throws Exception {
            given(userService.getUserProfile(2L, 1L)).willReturn(new UserProfileResponse(
                    2L, "Jane", "Smith", "bio", "NYC", "USA", null,
                    LocalDateTime.of(2026, 1, 1, 12, 0),
                    3, 12.5, 3600L, 100L, 900L, 4, 2,
                    List.of(), true, false, true));

            mockMvc.perform(get("/api/v1/users/2/profile").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    // Lombok/Jackson naming parity with the monolith:
                    .andExpect(jsonPath("$.followedByCurrentUser").value(true))
                    .andExpect(jsonPath("$.private").value(false))
                    .andExpect(jsonPath("$.canViewActivities").value(true))
                    .andExpect(jsonPath("$.isPrivate").doesNotExist())
                    .andExpect(jsonPath("$.totalDistance").value(12.5))
                    .andExpect(jsonPath("$.recentActivities").isArray());
        }

        @Test
        void sanitizedProfileHidesEverythingButIdentity() throws Exception {
            given(userService.getUserProfile(2L, 1L)).willReturn(
                    UserProfileResponse.sanitized(2L, "Jane", "Smith", null));

            mockMvc.perform(get("/api/v1/users/2/profile").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.bio").isEmpty())
                    .andExpect(jsonPath("$.private").value(true))
                    .andExpect(jsonPath("$.canViewActivities").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/password and DELETE /api/v1/users/me")
    class PasswordAndDelete {

        @Test
        void changePasswordReturnsMessage() throws Exception {
            mockMvc.perform(put("/api/v1/users/me/password")
                            .header(USER_ID, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "currentPassword", "old", "newPassword", "new-password"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Password changed successfully"));

            verify(userService).changePassword(1L, "old", "new-password");
        }

        @Test
        void wrongCurrentPasswordSurfaces400Message() throws Exception {
            willThrow(new ApiRequestException("Current password is incorrect", HttpStatus.BAD_REQUEST))
                    .given(userService).changePassword(eq(1L), anyString(), anyString());

            mockMvc.perform(put("/api/v1/users/me/password")
                            .header(USER_ID, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "currentPassword", "wrong", "newPassword", "new-password"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        }

        @Test
        void deleteAccountReturnsMessage() throws Exception {
            mockMvc.perform(delete("/api/v1/users/me").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Account deleted successfully"));

            verify(userService).deleteAccount(1L);
        }
    }
}
