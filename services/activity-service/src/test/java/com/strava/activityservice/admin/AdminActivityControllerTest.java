package com.strava.activityservice.admin;

import com.strava.activityservice.activity.ActivityService;
import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.exception.ApiRequestException;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.strava.activityservice.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
        when(activityService.listForAdmin(isNull(), any(Pageable.class)))
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
    @DisplayName("search is passed through to the service")
    void searchDelegates() throws Exception {
        when(activityService.listForAdmin(eq("run"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userCacheService.usersFor(anyCollection())).thenReturn(Map.of());

        mockMvc.perform(get("/api/v1/admin/activities")
                        .param("search", "run")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk());

        verify(activityService).listForAdmin(eq("run"), any(Pageable.class));
    }

    @Test
    @DisplayName("DELETE removes the activity (with PRs) and answers with main's message body")
    void deleteActivity() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/activities/1")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activity deleted"));

        verify(activityService).deleteAsAdmin(1L);
    }

    @Test
    @DisplayName("DELETE of a missing activity is a 404 ProblemDetail")
    void deleteMissingActivity() throws Exception {
        doThrow(new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND))
                .when(activityService).deleteAsAdmin(999L);

        mockMvc.perform(delete("/api/v1/admin/activities/999")
                        .header("X-User-Id", "4")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Activity not found"));
    }
}
