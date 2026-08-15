package com.strava.userservice.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.user.ActivityStatsPort;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract + RBAC tests for {@code /api/v1/admin/users/**}. {@link SecurityConfig} is
 * imported so the {@code hasRole("ADMIN")} rule is enforced from the {@code X-User-Roles}
 * header, exactly as behind the gateway.
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminController")
class AdminControllerTest {

    private static final String USER_ID = "X-User-Id";
    private static final String USER_ROLES = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UserFollowRepository followRepository;

    @MockitoBean
    private ActivityStatsPort activityStats;

    private User user(long id) {
        return User.builder()
                .id(id).firstName("John").lastName("Doe").email("john@example.com")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();
    }

    @Nested
    @DisplayName("role enforcement")
    class RoleEnforcement {

        @Test
        void plainUserIsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                            .header(USER_ID, "1").header(USER_ROLES, "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void anonymousIsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class ListUsers {

        @Test
        void returnsHeaderResponseEnvelopeAsBody() throws Exception {
            given(adminService.getAllUsers(any(Pageable.class))).willReturn(
                    new PageImpl<>(List.of(user(1L)), PageRequest.of(0, 20), 1));
            given(followRepository.countByUserId(1L)).willReturn(2L);
            given(followRepository.countByFollowerId(1L)).willReturn(3L);
            given(activityStats.countActivities(1L)).willReturn(7);

            mockMvc.perform(get("/api/v1/admin/users")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN"))
                    .andExpect(status().isOk())
                    // The frontend reads response.data.items from the envelope body.
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items[0].id").value(1))
                    .andExpect(jsonPath("$.items[0].email").value("john@example.com"))
                    .andExpect(jsonPath("$.items[0].role").value("USER"))
                    .andExpect(jsonPath("$.items[0].active").value(true))
                    .andExpect(jsonPath("$.items[0].followersCount").value(2))
                    .andExpect(jsonPath("$.items[0].followingCount").value(3))
                    .andExpect(jsonPath("$.items[0].activitiesCount").value(7))
                    .andExpect(jsonPath("$.headers['page-total-count'][0]").value("1"))
                    .andExpect(jsonPath("$.headers['page-total-elements'][0]").value("1"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users/{userId}")
    class GetUser {

        @Test
        void returnsSingleAdminUser() throws Exception {
            given(adminService.getUserById(1L)).willReturn(user(1L));
            given(followRepository.countByUserId(1L)).willReturn(0L);
            given(followRepository.countByFollowerId(1L)).willReturn(0L);
            given(activityStats.countActivities(1L)).willReturn(0);

            mockMvc.perform(get("/api/v1/admin/users/1")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.authProvider").value("LOCAL"));
        }

        @Test
        void unknownUserIs404() throws Exception {
            given(adminService.getUserById(99L))
                    .willThrow(new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/admin/users/99")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(USER_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("mutations")
    class Mutations {

        @Test
        void updateRoleReturnsMessage() throws Exception {
            mockMvc.perform(put("/api/v1/admin/users/1/role")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User role updated"));

            verify(adminService).updateUserRole(1L, Role.ADMIN);
        }

        @Test
        void missingRoleIsBadRequest() throws Exception {
            mockMvc.perform(put("/api/v1/admin/users/1/role")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.role").value("Role is required"));
        }

        @Test
        void toggleActiveReturnsMessage() throws Exception {
            mockMvc.perform(put("/api/v1/admin/users/1/toggle-active")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User active status toggled"));

            verify(adminService).toggleUserActive(1L);
        }

        @Test
        void deleteUserReturnsMessage() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/users/1")
                            .header(USER_ID, "4").header(USER_ROLES, "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User deleted"));

            verify(adminService).deleteUser(1L);
        }
    }
}
