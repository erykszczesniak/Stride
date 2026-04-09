package com.strava.backend.controller;

import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.NotificationService;
import com.strava.backend.service.WeeklySummaryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
@ActiveProfiles("test")
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private WeeklySummaryService weeklySummaryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private NotificationDTO testNotificationDTO;

    @BeforeEach
    void setUp() {
        testNotificationDTO = NotificationDTO.builder()
                .id(1L)
                .senderFirstName("Bob")
                .senderLastName("Jones")
                .type(NotificationType.LIKE)
                .message("Bob liked your activity")
                .referenceId(100L)
                .referenceType("ACTIVITY")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        setUpUserAuth();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpUserAuth() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setUpAdminAuth() {
        UserPrincipal adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@example.com")
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("GET /api/v1/notifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return list of notifications")
        void shouldReturnNotifications() throws Exception {
            given(notificationService.getNotifications(1L))
                    .willReturn(List.of(testNotificationDTO));

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].message").value("Bob liked your activity"))
                    .andExpect(jsonPath("$[0].senderFirstName").value("Bob"))
                    .andExpect(jsonPath("$[0].type").value("LIKE"));
        }

        @Test
        @DisplayName("Should return empty list when no notifications")
        void shouldReturnEmptyList() throws Exception {
            given(notificationService.getNotifications(1L))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread count")
        void shouldReturnUnreadCount() throws Exception {
            given(notificationService.getUnreadCount(1L)).willReturn(5L);

            mockMvc.perform(get("/api/v1/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(5));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/{notificationId}/read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkAsRead() throws Exception {
            willDoNothing().given(notificationService).markAsRead(1L, 1L);

            mockMvc.perform(put("/api/v1/notifications/1/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification marked as read"));
        }

        @Test
        @DisplayName("Should return 404 when notification not found")
        void shouldReturn404WhenNotFound() throws Exception {
            willThrow(new ApiRequestException("Notification not found", HttpStatus.NOT_FOUND))
                    .given(notificationService).markAsRead(999L, 1L);

            mockMvc.perform(put("/api/v1/notifications/999/read"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when not the recipient")
        void shouldReturn403WhenNotRecipient() throws Exception {
            willThrow(new ApiRequestException("Access denied", HttpStatus.FORBIDDEN))
                    .given(notificationService).markAsRead(1L, 1L);

            mockMvc.perform(put("/api/v1/notifications/1/read"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/read-all")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all as read")
        void shouldMarkAllAsRead() throws Exception {
            willDoNothing().given(notificationService).markAllAsRead(1L);

            mockMvc.perform(put("/api/v1/notifications/read-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("All notifications marked as read"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications/{notificationId}")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification successfully")
        void shouldDeleteNotification() throws Exception {
            willDoNothing().given(notificationService).deleteNotification(1L, 1L);

            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notification deleted"));
        }

        @Test
        @DisplayName("Should return 404 when notification not found")
        void shouldReturn404WhenNotFound() throws Exception {
            willThrow(new ApiRequestException("Notification not found", HttpStatus.NOT_FOUND))
                    .given(notificationService).deleteNotification(999L, 1L);

            mockMvc.perform(delete("/api/v1/notifications/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when not the recipient")
        void shouldReturn403WhenNotRecipient() throws Exception {
            willThrow(new ApiRequestException("Access denied", HttpStatus.FORBIDDEN))
                    .given(notificationService).deleteNotification(1L, 1L);

            mockMvc.perform(delete("/api/v1/notifications/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notifications")
    class ClearAllNotificationsTests {

        @Test
        @DisplayName("Should clear all notifications")
        void shouldClearAllNotifications() throws Exception {
            willDoNothing().given(notificationService).clearAllNotifications(1L);

            mockMvc.perform(delete("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("All notifications cleared"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/notifications/weekly-summary/trigger")
    class TriggerWeeklySummaryTests {

        @Test
        @DisplayName("Should return 200 for admin user")
        void shouldReturn200ForAdmin() throws Exception {
            setUpAdminAuth();
            willDoNothing().given(weeklySummaryService).sendWeeklySummaries();

            mockMvc.perform(post("/api/v1/notifications/weekly-summary/trigger"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Weekly summary triggered"));
        }

        @Test
        @DisplayName("Should return 403 for non-admin user")
        void shouldReturn403ForNonAdmin() throws Exception {
            mockMvc.perform(post("/api/v1/notifications/weekly-summary/trigger"))
                    .andExpect(status().isForbidden());
        }
    }
}
