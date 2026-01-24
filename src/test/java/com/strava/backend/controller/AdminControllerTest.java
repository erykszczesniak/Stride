package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.User;
import com.strava.backend.dto.admin.AdminStatsResponse;
import com.strava.backend.dto.admin.AdminUserResponse;
import com.strava.backend.dto.admin.UpdateUserRoleRequest;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.AdminService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AdminController Unit Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private User testUser;
    private AdminStatsResponse adminStats;
    private AdminUserResponse adminUserResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testUser = TestDataFactory.createUser();

        adminStats = AdminStatsResponse.builder()
                .totalUsers(100)
                .activeUsers(95)
                .totalActivities(1500)
                .totalClubs(25)
                .totalChallenges(50)
                .totalEvents(30)
                .newUsersThisWeek(10)
                .activitiesThisWeek(200)
                .build();

        adminUserResponse = new AdminUserResponse();
        adminUserResponse.setId(1L);
        adminUserResponse.setFirstName("John");
        adminUserResponse.setLastName("Doe");
        adminUserResponse.setEmail("john@example.com");
        adminUserResponse.setAuthProvider(AuthProvider.LOCAL);
        adminUserResponse.setRole(Role.USER);
        adminUserResponse.setActive(true);
        adminUserResponse.setCreatedAt(LocalDateTime.now());
        adminUserResponse.setActivitiesCount(10);
        adminUserResponse.setFollowersCount(5);
        adminUserResponse.setFollowingCount(8);
    }

    @Nested
    @DisplayName("GET /api/v1/admin/stats")
    class GetAdminStatsTests {

        @Test
        @DisplayName("Should return admin statistics")
        void shouldReturnAdminStatistics() throws Exception {
            given(adminService.getAdminStats()).willReturn(adminStats);

            mockMvc.perform(get("/api/v1/admin/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalUsers").value(100))
                    .andExpect(jsonPath("$.activeUsers").value(95))
                    .andExpect(jsonPath("$.totalActivities").value(1500))
                    .andExpect(jsonPath("$.totalClubs").value(25))
                    .andExpect(jsonPath("$.totalChallenges").value(50))
                    .andExpect(jsonPath("$.totalEvents").value(30))
                    .andExpect(jsonPath("$.newUsersThisWeek").value(10))
                    .andExpect(jsonPath("$.activitiesThisWeek").value(200));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return paginated users")
        void shouldReturnPaginatedUsers() throws Exception {
            Page<User> userPage = new PageImpl<>(List.of(testUser));
            given(adminService.getAllUsers(any(Pageable.class))).willReturn(userPage);
            given(commonMapper.convertToResponse(testUser, AdminUserResponse.class)).willReturn(adminUserResponse);
            given(activityRepository.countByUserId(1L)).willReturn(10);
            given(userRepository.countFollowers(1L)).willReturn(5);
            given(userRepository.countFollowing(1L)).willReturn(8);

            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].id").value(1))
                    .andExpect(jsonPath("$.items[0].firstName").value("John"))
                    .andExpect(jsonPath("$.items[0].email").value("john@example.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user by ID")
        void shouldReturnUserById() throws Exception {
            given(adminService.getUserById(1L)).willReturn(testUser);
            given(commonMapper.convertToResponse(testUser, AdminUserResponse.class)).willReturn(adminUserResponse);
            given(activityRepository.countByUserId(1L)).willReturn(10);
            given(userRepository.countFollowers(1L)).willReturn(5);
            given(userRepository.countFollowing(1L)).willReturn(8);

            mockMvc.perform(get("/api/v1/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.activitiesCount").value(10))
                    .andExpect(jsonPath("$.followersCount").value(5))
                    .andExpect(jsonPath("$.followingCount").value(8));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            given(adminService.getUserById(99L))
                    .willThrow(new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/admin/users/99"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/role")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should update user role")
        void shouldUpdateUserRole() throws Exception {
            UpdateUserRoleRequest request = new UpdateUserRoleRequest();
            request.setRole(Role.ADMIN);

            willDoNothing().given(adminService).updateUserRole(1L, Role.ADMIN);

            mockMvc.perform(put("/api/v1/admin/users/1/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User role updated"));
        }

        @Test
        @DisplayName("Should return bad request when role is null")
        void shouldReturnBadRequestWhenRoleIsNull() throws Exception {
            UpdateUserRoleRequest request = new UpdateUserRoleRequest();
            request.setRole(null);

            mockMvc.perform(put("/api/v1/admin/users/1/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/users/{userId}/toggle-active")
    class ToggleUserActiveTests {

        @Test
        @DisplayName("Should toggle user active status")
        void shouldToggleUserActiveStatus() throws Exception {
            willDoNothing().given(adminService).toggleUserActive(1L);

            mockMvc.perform(put("/api/v1/admin/users/1/toggle-active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User active status toggled"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{userId}")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() throws Exception {
            willDoNothing().given(adminService).deleteUser(1L);

            mockMvc.perform(delete("/api/v1/admin/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deleted"));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent user")
        void shouldReturn404WhenDeletingNonExistentUser() throws Exception {
            willThrow(new ApiRequestException("User not found", HttpStatus.NOT_FOUND))
                    .given(adminService).deleteUser(99L);

            mockMvc.perform(delete("/api/v1/admin/users/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
