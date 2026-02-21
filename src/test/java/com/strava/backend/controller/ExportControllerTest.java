package com.strava.backend.controller;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ExportController Unit Tests")
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Activity testActivity;
    private User testUser;
    private ActivityResponse activityResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        testActivity = TestDataFactory.createActivity(1L, testUser);
        testActivity.setStartTime(LocalDateTime.of(2024, 1, 15, 8, 30));

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setTitle("Morning Run");
        activityResponse.setActivityType(ActivityType.RUN);
        activityResponse.setDistance(10.5);
        activityResponse.setDuration(3600);

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/export/activities/json")
    class ExportActivitiesAsJsonTests {

        @Test
        @DisplayName("Should export activities as JSON")
        void shouldExportActivitiesAsJson() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testActivity));
            given(commonMapper.convertToResponseList(List.of(testActivity), ActivityResponse.class))
                    .willReturn(List.of(activityResponse));

            mockMvc.perform(get("/api/v1/export/activities/json"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".json")));
        }

        @Test
        @DisplayName("Should export empty JSON when no activities")
        void shouldExportEmptyJsonWhenNoActivities() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(Collections.emptyList());
            given(commonMapper.convertToResponseList(Collections.emptyList(), ActivityResponse.class))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/export/activities/json"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testActivity));
            given(commonMapper.convertToResponseList(List.of(testActivity), ActivityResponse.class))
                    .willReturn(List.of(activityResponse));

            mockMvc.perform(get("/api/v1/export/activities/json"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/export/activities/csv")
    class ExportActivitiesAsCsvTests {

        @Test
        @DisplayName("Should export activities as CSV")
        void shouldExportActivitiesAsCsv() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testActivity));

            mockMvc.perform(get("/api/v1/export/activities/csv"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/csv"))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".csv")));
        }

        @Test
        @DisplayName("Should include CSV headers")
        void shouldIncludeCsvHeaders() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/export/activities/csv"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("ID,Title,Activity Type")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/export/activities/txt")
    class ExportActivitiesAsTxtTests {

        @Test
        @DisplayName("Should export activities as TXT")
        void shouldExportActivitiesAsTxt() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testActivity));

            mockMvc.perform(get("/api/v1/export/activities/txt"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".txt")));
        }

        @Test
        @DisplayName("Should include report headers")
        void shouldIncludeReportHeaders() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/export/activities/txt"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("ACTIVITIES EXPORT REPORT")));
        }

        @Test
        @DisplayName("Should include total activities count")
        void shouldIncludeTotalActivitiesCount() throws Exception {
            given(activityRepository.findByUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(testActivity));

            mockMvc.perform(get("/api/v1/export/activities/txt"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Total activities: 1")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/export/activities/gpx/{activityId}")
    class ExportActivityAsGpxTests {

        @Test
        @DisplayName("Should export activity as GPX")
        void shouldExportActivityAsGpx() throws Exception {
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));

            mockMvc.perform(get("/api/v1/export/activities/gpx/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".gpx")));
        }

        @Test
        @DisplayName("Should include GPX metadata")
        void shouldIncludeGpxMetadata() throws Exception {
            given(activityRepository.findById(1L)).willReturn(Optional.of(testActivity));

            mockMvc.perform(get("/api/v1/export/activities/gpx/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("<?xml version=\"1.0\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("<gpx version=\"1.1\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Morning Run")));
        }

        @Test
        @DisplayName("Should return 500 when activity not found")
        void shouldReturn500WhenActivityNotFound() throws Exception {
            given(activityRepository.findById(99L)).willReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/export/activities/gpx/99"))
                    .andExpect(status().isInternalServerError());
        }
    }
}
