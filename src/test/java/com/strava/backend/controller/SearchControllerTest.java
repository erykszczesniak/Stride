package com.strava.backend.controller;

import com.strava.backend.domain.*;
import com.strava.backend.dto.SearchResponse;
import com.strava.backend.dto.activity.ActivityResponse;
import com.strava.backend.dto.challenge.ChallengeResponse;
import com.strava.backend.dto.club.ClubResponse;
import com.strava.backend.dto.event.EventResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.repository.*;
import com.strava.backend.repository.projection.UserProjection;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ActivityRepository activityRepository;

    @MockitoBean
    private ClubRepository clubRepository;

    @MockitoBean
    private ChallengeRepository challengeRepository;

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Activity testActivity;
    private Club testClub;
    private Challenge testChallenge;
    private Event testEvent;
    private UserProjection testUserProjection;

    private BaseUserResponse userResponse;
    private ActivityResponse activityResponse;
    private ClubResponse clubResponse;
    private ChallengeResponse challengeResponse;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        testActivity = TestDataFactory.createActivity(1L, testUser);
        testClub = TestDataFactory.createClub(1L, testUser);
        testChallenge = TestDataFactory.createChallenge(1L, testUser);
        testEvent = TestDataFactory.createEvent(1L, testUser);

        // Create mock UserProjection
        testUserProjection = mock(UserProjection.class);
        given(testUserProjection.getId()).willReturn(1L);
        given(testUserProjection.getFirstName()).willReturn("John");
        given(testUserProjection.getLastName()).willReturn("Doe");

        userResponse = new BaseUserResponse();
        userResponse.setId(1L);
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");

        activityResponse = new ActivityResponse();
        activityResponse.setId(1L);
        activityResponse.setTitle("Morning Run");

        clubResponse = new ClubResponse();
        clubResponse.setId(1L);
        clubResponse.setName("Running Club");

        challengeResponse = new ChallengeResponse();
        challengeResponse.setId(1L);
        challengeResponse.setName("Monthly Running Challenge");

        eventResponse = new EventResponse();
        eventResponse.setId(1L);
        eventResponse.setName("Marathon 2024");
    }

    @Nested
    @DisplayName("GET /api/v1/search")
    class GlobalSearchTests {

        @Test
        @DisplayName("Should return global search results")
        void shouldReturnGlobalSearchResults() throws Exception {
            Page<UserProjection> userPage = new PageImpl<>(List.of(testUserProjection));
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity));
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));

            given(userRepository.searchUsers(eq("test"), any(PageRequest.class))).willReturn(userPage);
            given(activityRepository.searchActivities(eq("test"), any(PageRequest.class))).willReturn(activityPage);
            given(clubRepository.searchClubs(eq("test"), any(PageRequest.class))).willReturn(clubPage);
            given(challengeRepository.searchChallenges(eq("test"), any(PageRequest.class))).willReturn(challengePage);
            given(eventRepository.searchEvents(eq("test"), any(PageRequest.class))).willReturn(eventPage);

            given(commonMapper.convertToResponseList(anyList(), eq(BaseUserResponse.class)))
                    .willReturn(List.of(userResponse));
            given(commonMapper.convertToResponseList(anyList(), eq(ActivityResponse.class)))
                    .willReturn(List.of(activityResponse));
            given(commonMapper.convertToResponseList(anyList(), eq(ClubResponse.class)))
                    .willReturn(List.of(clubResponse));
            given(commonMapper.convertToResponseList(anyList(), eq(ChallengeResponse.class)))
                    .willReturn(List.of(challengeResponse));
            given(commonMapper.convertToResponseList(anyList(), eq(EventResponse.class)))
                    .willReturn(List.of(eventResponse));

            mockMvc.perform(get("/api/v1/search")
                            .param("query", "test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users[0].id").value(1))
                    .andExpect(jsonPath("$.activities[0].id").value(1))
                    .andExpect(jsonPath("$.clubs[0].id").value(1))
                    .andExpect(jsonPath("$.challenges[0].id").value(1))
                    .andExpect(jsonPath("$.events[0].id").value(1));
        }

        @Test
        @DisplayName("Should return empty results when no matches")
        void shouldReturnEmptyResultsWhenNoMatches() throws Exception {
            Page<UserProjection> emptyUserPage = new PageImpl<>(Collections.emptyList());
            Page<Activity> emptyActivityPage = new PageImpl<>(Collections.emptyList());
            Page<Club> emptyClubPage = new PageImpl<>(Collections.emptyList());
            Page<Challenge> emptyChallengePage = new PageImpl<>(Collections.emptyList());
            Page<Event> emptyEventPage = new PageImpl<>(Collections.emptyList());

            given(userRepository.searchUsers(eq("xyz"), any(PageRequest.class))).willReturn(emptyUserPage);
            given(activityRepository.searchActivities(eq("xyz"), any(PageRequest.class))).willReturn(emptyActivityPage);
            given(clubRepository.searchClubs(eq("xyz"), any(PageRequest.class))).willReturn(emptyClubPage);
            given(challengeRepository.searchChallenges(eq("xyz"), any(PageRequest.class))).willReturn(emptyChallengePage);
            given(eventRepository.searchEvents(eq("xyz"), any(PageRequest.class))).willReturn(emptyEventPage);

            given(commonMapper.convertToResponseList(anyList(), eq(BaseUserResponse.class)))
                    .willReturn(Collections.emptyList());
            given(commonMapper.convertToResponseList(anyList(), eq(ActivityResponse.class)))
                    .willReturn(Collections.emptyList());
            given(commonMapper.convertToResponseList(anyList(), eq(ClubResponse.class)))
                    .willReturn(Collections.emptyList());
            given(commonMapper.convertToResponseList(anyList(), eq(ChallengeResponse.class)))
                    .willReturn(Collections.emptyList());
            given(commonMapper.convertToResponseList(anyList(), eq(EventResponse.class)))
                    .willReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/search")
                            .param("query", "xyz"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isEmpty())
                    .andExpect(jsonPath("$.activities").isEmpty())
                    .andExpect(jsonPath("$.clubs").isEmpty())
                    .andExpect(jsonPath("$.challenges").isEmpty())
                    .andExpect(jsonPath("$.events").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/users")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users")
        void shouldSearchUsers() throws Exception {
            Page<UserProjection> userPage = new PageImpl<>(List.of(testUserProjection));
            given(userRepository.searchUsers(eq("John"), any(PageRequest.class))).willReturn(userPage);
            given(commonMapper.convertToResponseList(anyList(), eq(BaseUserResponse.class)))
                    .willReturn(List.of(userResponse));

            mockMvc.perform(get("/api/v1/search/users")
                            .param("query", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/activities")
    class SearchActivitiesTests {

        @Test
        @DisplayName("Should search activities")
        void shouldSearchActivities() throws Exception {
            Page<Activity> activityPage = new PageImpl<>(List.of(testActivity));
            given(activityRepository.searchActivities(eq("Run"), any(PageRequest.class))).willReturn(activityPage);
            given(commonMapper.convertToResponseList(List.of(testActivity), ActivityResponse.class))
                    .willReturn(List.of(activityResponse));

            mockMvc.perform(get("/api/v1/search/activities")
                            .param("query", "Run"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Morning Run"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/clubs")
    class SearchClubsTests {

        @Test
        @DisplayName("Should search clubs")
        void shouldSearchClubs() throws Exception {
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubRepository.searchClubs(eq("Running"), any(PageRequest.class))).willReturn(clubPage);
            given(commonMapper.convertToResponseList(List.of(testClub), ClubResponse.class))
                    .willReturn(List.of(clubResponse));

            mockMvc.perform(get("/api/v1/search/clubs")
                            .param("query", "Running"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Running Club"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/challenges")
    class SearchChallengesTests {

        @Test
        @DisplayName("Should search challenges")
        void shouldSearchChallenges() throws Exception {
            Page<Challenge> challengePage = new PageImpl<>(List.of(testChallenge));
            given(challengeRepository.searchChallenges(eq("Monthly"), any(PageRequest.class))).willReturn(challengePage);
            given(commonMapper.convertToResponseList(List.of(testChallenge), ChallengeResponse.class))
                    .willReturn(List.of(challengeResponse));

            mockMvc.perform(get("/api/v1/search/challenges")
                            .param("query", "Monthly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Monthly Running Challenge"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/search/events")
    class SearchEventsTests {

        @Test
        @DisplayName("Should search events")
        void shouldSearchEvents() throws Exception {
            Page<Event> eventPage = new PageImpl<>(List.of(testEvent));
            given(eventRepository.searchEvents(eq("Marathon"), any(PageRequest.class))).willReturn(eventPage);
            given(commonMapper.convertToResponseList(List.of(testEvent), EventResponse.class))
                    .willReturn(List.of(eventResponse));

            mockMvc.perform(get("/api/v1/search/events")
                            .param("query", "Marathon"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Marathon 2024"));
        }
    }
}
