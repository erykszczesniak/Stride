package com.strava.activityservice.admin;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.domain.ActivityRepository;
import com.strava.activityservice.user.BaseUserResponse;
import com.strava.activityservice.user.UserCacheService;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminActivityController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminActivityController Unit Tests")
class AdminActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private UserCacheService userCacheService;

    @Test
    @DisplayName("non-admin callers get 403 from the /api/v1/admin/** rule")
    void nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/activities/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Roles", "USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/activities ships the HeaderResponse envelope as the body")
    void listShipsEnvelopeBody() throws Exception {
        Activity activity = TestDataFactory.createActivity(1L, 5L);
        when(activityRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(activity), Pageable.ofSize(20), 1));
        BaseUserResponse author = new BaseUserResponse();
        author.setId(5L);
        author.setFirstName("John");
        author.setLastName("Doe");
        when(userCacheService.usersFor(anyCollection())).thenReturn(Map.of(5L, author));

        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Morning Run"))
                .andExpect(jsonPath("$.items[0].user.fullName").value("John Doe"))
                .andExpect(jsonPath("$.headers['page-total-count'][0]").value("1"))
                .andExpect(jsonPath("$.headers['page-total-elements'][0]").value("1"));
    }

    @Test
    @DisplayName("search delegates to the title search query")
    void searchDelegates() throws Exception {
        when(activityRepository.searchByTitle(eq("run"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userCacheService.usersFor(anyCollection())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/admin/activities")
                        .param("search", "run")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());

        verify(activityRepository).searchByTitle(eq("run"), any(Pageable.class));
        verify(activityRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("DELETE removes the activity (with PRs) and answers with main's message body")
    void deleteActivity() throws Exception {
        Activity activity = TestDataFactory.createActivity(1L, 5L);
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        mockMvc.perform(delete("/api/v1/admin/activities/1")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activity deleted"));

        verify(activityService).deleteWithPersonalRecords(activity);
    }

    @Test
    @DisplayName("DELETE of a missing activity is a 404 ProblemDetail")
    void deleteMissingActivity() throws Exception {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/admin/activities/999")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found"));
    }
}
