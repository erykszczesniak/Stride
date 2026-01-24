package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.User;
import com.strava.backend.dto.HeaderResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.dto.user.UpdateUserRequest;
import com.strava.backend.dto.user.UserResponse;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.mapper.UserMapper;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.UserService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private UserResponse userResponse;
    private BaseUserResponse baseUserResponse;
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testUser = TestDataFactory.createUser(1L, "john@example.com", "John", "Doe");

        userResponse = new UserResponse();
        userResponse.setId(1L);
        userResponse.setEmail("john@example.com");
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");
        userResponse.setBio("Test bio");
        userResponse.setCity("Warsaw");
        userResponse.setCountry("Poland");

        baseUserResponse = new BaseUserResponse();
        baseUserResponse.setId(1L);
        baseUserResponse.setFirstName("John");
        baseUserResponse.setLastName("Doe");
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user")
        void shouldReturnCurrentUser() throws Exception {
            // Given
            given(userMapper.getCurrentUser(1L)).willReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/users/me")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            // Given
            given(userMapper.getCurrentUser(1L)).willReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() throws Exception {
            // Given
            given(userMapper.getUserById(1L)).willReturn(userResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        void shouldReturnNotFoundWhenUserNotExists() throws Exception {
            // Given
            given(userMapper.getUserById(999L))
                    .willThrow(new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

            // When/Then
            mockMvc.perform(get("/api/v1/users/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() throws Exception {
            // Given
            UserResponse user2 = new UserResponse();
            user2.setId(2L);
            user2.setEmail("jane@example.com");
            user2.setFirstName("Jane");
            user2.setLastName("Doe");

            HeaderResponse<UserResponse> response = HeaderResponse.of(
                    List.of(userResponse, user2), 2, 2L);
            given(userMapper.getAllUsers(any(Pageable.class))).willReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(2));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateCurrentUserTests {

        @Test
        @DisplayName("Should update current user successfully")
        void shouldUpdateCurrentUserSuccessfully() throws Exception {
            // Given
            UpdateUserRequest request = TestDataFactory.createUpdateUserRequest();
            userResponse.setFirstName("Jane");
            userResponse.setLastName("Smith");

            given(userMapper.updateUser(eq(1L), any(UpdateUserRequest.class), any()))
                    .willReturn(userResponse);

            // When/Then
            mockMvc.perform(put("/api/v1/users/me")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/follow")
    class FollowUserTests {

        @Test
        @DisplayName("Should follow user successfully")
        void shouldFollowUserSuccessfully() throws Exception {
            // Given
            willDoNothing().given(userService).followUser(1L, 2L);

            // When/Then
            mockMvc.perform(post("/api/v1/users/2/follow")
                            .param("currentUserId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User followed"));
        }

        @Test
        @DisplayName("Should return error when already following")
        void shouldReturnErrorWhenAlreadyFollowing() throws Exception {
            // Given
            willThrow(new ApiRequestException("Already following this user", HttpStatus.BAD_REQUEST))
                    .given(userService).followUser(1L, 2L);

            // When/Then
            mockMvc.perform(post("/api/v1/users/2/follow")
                            .param("currentUserId", "1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            // Given
            willDoNothing().given(userService).followUser(1L, 2L);

            // When/Then
            mockMvc.perform(post("/api/v1/users/2/follow"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/users/{userId}/follow")
    class UnfollowUserTests {

        @Test
        @DisplayName("Should unfollow user successfully")
        void shouldUnfollowUserSuccessfully() throws Exception {
            // Given
            willDoNothing().given(userService).unfollowUser(1L, 2L);

            // When/Then
            mockMvc.perform(delete("/api/v1/users/2/follow")
                            .param("currentUserId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User unfollowed"));
        }

        @Test
        @DisplayName("Should return error when not following")
        void shouldReturnErrorWhenNotFollowing() throws Exception {
            // Given
            willThrow(new ApiRequestException("Not following this user", HttpStatus.BAD_REQUEST))
                    .given(userService).unfollowUser(1L, 2L);

            // When/Then
            mockMvc.perform(delete("/api/v1/users/2/follow")
                            .param("currentUserId", "1"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/followers")
    class GetFollowersTests {

        @Test
        @DisplayName("Should return followers list")
        void shouldReturnFollowersList() throws Exception {
            // Given
            User follower = TestDataFactory.createUser(2L, "jane@example.com", "Jane", "Smith");
            BaseUserResponse followerResponse = new BaseUserResponse();
            followerResponse.setId(2L);
            followerResponse.setFirstName("Jane");
            followerResponse.setLastName("Smith");

            given(userService.getFollowers(1L)).willReturn(List.of(follower));
            given(commonMapper.convertToResponse(follower, BaseUserResponse.class)).willReturn(followerResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/users/1/followers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(2))
                    .andExpect(jsonPath("$[0].firstName").value("Jane"));
        }

        @Test
        @DisplayName("Should return empty list when no followers")
        void shouldReturnEmptyListWhenNoFollowers() throws Exception {
            // Given
            given(userService.getFollowers(1L)).willReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/users/1/followers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/following")
    class GetFollowingTests {

        @Test
        @DisplayName("Should return following list")
        void shouldReturnFollowingList() throws Exception {
            // Given
            User following = TestDataFactory.createUser(3L, "bob@example.com", "Bob", "Brown");
            BaseUserResponse followingResponse = new BaseUserResponse();
            followingResponse.setId(3L);
            followingResponse.setFirstName("Bob");
            followingResponse.setLastName("Brown");

            given(userService.getFollowing(1L)).willReturn(List.of(following));
            given(commonMapper.convertToResponse(following, BaseUserResponse.class)).willReturn(followingResponse);

            // When/Then
            mockMvc.perform(get("/api/v1/users/1/following"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(3))
                    .andExpect(jsonPath("$[0].firstName").value("Bob"));
        }

        @Test
        @DisplayName("Should return empty list when not following anyone")
        void shouldReturnEmptyListWhenNotFollowing() throws Exception {
            // Given
            given(userService.getFollowing(1L)).willReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/v1/users/1/following"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
