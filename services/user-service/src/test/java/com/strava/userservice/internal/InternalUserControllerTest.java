package com.strava.userservice.internal;

import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the cluster-only {@code /internal/users} API. The response shapes
 * asserted here are what other services build against — treat failures as breaking
 * changes, not test rot. No identity headers are sent: /internal/** is permitAll
 * (network-level protection), which these tests also verify.
 */
@WebMvcTest(InternalUserController.class)
@Import(SecurityConfig.class)
@DisplayName("InternalUserController")
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserFollowRepository followRepository;

    private User user(long id, String firstName, String lastName) {
        return User.builder()
                .id(id).firstName(firstName).lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .avatar("http://avatar/" + id)
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    @Nested
    @DisplayName("GET /internal/users?ids=")
    class BatchLookup {

        @Test
        void returnsExactSummaryShape() throws Exception {
            given(userRepository.findByIdIn(List.of(1L, 2L))).willReturn(List.of(
                    user(1L, "John", "Doe"), user(2L, "Jane", "Smith")));

            mockMvc.perform(get("/internal/users").param("ids", "1,2"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            [
                              {"id":1,"firstName":"John","lastName":"Doe","fullName":"John Doe","avatar":"http://avatar/1"},
                              {"id":2,"firstName":"Jane","lastName":"Smith","fullName":"Jane Smith","avatar":"http://avatar/2"}
                            ]
                            """, true));
        }

        @Test
        void unknownIdsAreOmitted() throws Exception {
            given(userRepository.findByIdIn(List.of(1L, 99L)))
                    .willReturn(List.of(user(1L, "John", "Doe")));

            mockMvc.perform(get("/internal/users").param("ids", "1,99"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /internal/users/{id}/visibility")
    class Visibility {

        @Test
        void privateProfileWithFollowingViewer() throws Exception {
            User owner = user(1L, "John", "Doe");
            owner.setIsPrivate(true);
            given(userRepository.findById(1L)).willReturn(Optional.of(owner));
            given(followRepository.existsByUserIdAndFollowerId(1L, 2L)).willReturn(true);

            mockMvc.perform(get("/internal/users/1/visibility").param("viewerId", "2"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"isPrivate\":true,\"viewerFollows\":true}", true));
        }

        @Test
        void missingViewerMeansNotFollowing() throws Exception {
            given(userRepository.findById(1L)).willReturn(Optional.of(user(1L, "John", "Doe")));

            mockMvc.perform(get("/internal/users/1/visibility"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"isPrivate\":false,\"viewerFollows\":false}", true));
        }

        @Test
        void unknownUserIs404() throws Exception {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            mockMvc.perform(get("/internal/users/99/visibility"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /internal/users/{id}/notification-profile")
    class NotificationProfile {

        @Test
        void returnsExactNotificationProfileShape() throws Exception {
            User u = user(1L, "John", "Doe");
            u.setWeeklySummary(false);
            given(userRepository.findById(1L)).willReturn(Optional.of(u));

            mockMvc.perform(get("/internal/users/1/notification-profile"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("""
                            {"id":1,"email":"john@example.com","firstName":"John","lastName":"Doe",
                             "fullName":"John Doe","avatar":"http://avatar/1",
                             "emailNotifications":true,"pushNotifications":true,
                             "weeklySummary":false,"active":true}
                            """, true));
        }
    }

    @Nested
    @DisplayName("GET /internal/users/weekly-summary-recipients")
    class WeeklySummaryRecipients {

        @Test
        void returnsSpringPageOfNotificationProfiles() throws Exception {
            given(userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(0, 100)))
                    .willReturn(new PageImpl<>(List.of(user(1L, "John", "Doe")),
                            PageRequest.of(0, 100), 1));

            mockMvc.perform(get("/internal/users/weekly-summary-recipients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].weeklySummary").value(true))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        void honorsPageAndSizeParams() throws Exception {
            given(userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(2, 50)))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));

            mockMvc.perform(get("/internal/users/weekly-summary-recipients")
                            .param("page", "2").param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }
}
