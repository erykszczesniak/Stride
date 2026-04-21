package com.strava.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.StravaConnection;
import com.strava.backend.domain.StravaImportedActivity;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.StravaConnectionRepository;
import com.strava.backend.repository.StravaImportedActivityRepository;
import com.strava.backend.service.ActivityService;
import com.strava.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the two pieces that are hardest to get right by hand:
 * <ul>
 *     <li>Access-token refresh — a still-valid token must be returned without a network call,
 *         an expired one must hit Strava's token endpoint.</li>
 *     <li>Dedupe — importing the same Strava activity id twice must only create one Activity.</li>
 * </ul>
 * End-to-end flow against a live Strava account is intentionally not covered.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StravaIntegrationService")
class StravaIntegrationServiceTest {

    @Mock private StravaStateSigner stateSigner;
    @Mock private StravaConnectionRepository connectionRepository;
    @Mock private StravaImportedActivityRepository importedRepository;
    @Mock private UserService userService;
    @Mock private ActivityService activityService;
    @Mock private ApplicationContext applicationContext;
    @Mock private RestTemplate restTemplate;
    private StravaTokenExchanger tokenExchanger;

    private StravaProperties stravaProperties;
    private StravaIntegrationServiceImpl service;
    private StravaImportRunner runner;

    @BeforeEach
    void setUp() {
        stravaProperties = new StravaProperties();
        stravaProperties.setClientId("client-id");
        stravaProperties.setClientSecret("client-secret");
        stravaProperties.setRedirectUri("http://localhost/cb");

        tokenExchanger = new StravaTokenExchanger(stravaProperties, new ObjectMapper());
        ReflectionTestUtils.setField(tokenExchanger, "restTemplate", restTemplate);

        service = new StravaIntegrationServiceImpl(
                stravaProperties,
                stateSigner,
                tokenExchanger,
                connectionRepository,
                importedRepository,
                userService,
                new ObjectMapper(),
                applicationContext);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    // ---------------------------------------------------------------------
    // Token refresh
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("refreshTokenIfNeeded")
    class RefreshTokenIfNeeded {

        @Test
        @DisplayName("returns the existing access token without hitting Strava when not expired")
        void returnsExistingTokenWhenStillValid() {
            StravaConnection valid = StravaConnection.builder()
                    .accessToken("still-good")
                    .refreshToken("r")
                    .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            when(connectionRepository.findByUserId(7L)).thenReturn(Optional.of(valid));

            String token = service.refreshTokenIfNeeded(7L);

            assertThat(token).isEqualTo("still-good");
            verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class),
                    any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("calls the Strava token endpoint and persists new tokens when expired")
        void refreshesExpiredToken() {
            StravaConnection expired = StravaConnection.builder()
                    .user(new User())
                    .accessToken("old")
                    .refreshToken("old-refresh")
                    .tokenExpiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            when(connectionRepository.findByUserId(7L)).thenReturn(Optional.of(expired));

            long newExpiresAt = System.currentTimeMillis() / 1000L + 3_600;
            String responseBody = String.format("""
                    {
                      "access_token": "new-access",
                      "refresh_token": "new-refresh",
                      "expires_at": %d,
                      "scope": "read,activity:read_all",
                      "athlete": { "id": 42 }
                    }
                    """, newExpiresAt);
            when(restTemplate.exchange(
                    eq("https://www.strava.com/api/v3/oauth/token"),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
            when(connectionRepository.save(any(StravaConnection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            String token = service.refreshTokenIfNeeded(7L);

            assertThat(token).isEqualTo("new-access");
            ArgumentCaptor<StravaConnection> saved = ArgumentCaptor.forClass(StravaConnection.class);
            verify(connectionRepository).save(saved.capture());
            assertThat(saved.getValue().getAccessToken()).isEqualTo("new-access");
            assertThat(saved.getValue().getRefreshToken()).isEqualTo("new-refresh");
            assertThat(saved.getValue().getStravaAthleteId()).isEqualTo(42L);
            assertThat(saved.getValue().isTokenExpired()).isFalse();
        }

        @Test
        @DisplayName("throws when no connection exists for the user")
        void throwsWhenMissingConnection() {
            when(connectionRepository.findByUserId(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.refreshTokenIfNeeded(42L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("No Strava connection");
        }

        @Test
        @DisplayName("throws 401 when connection has no refresh token available")
        void throwsWhenNoRefreshToken() {
            StravaConnection brokenConn = StravaConnection.builder()
                    .accessToken("x")
                    .refreshToken(null)
                    .tokenExpiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            when(connectionRepository.findByUserId(9L)).thenReturn(Optional.of(brokenConn));

            assertThatThrownBy(() -> service.refreshTokenIfNeeded(9L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessageContaining("reconnect");
        }
    }

    // ---------------------------------------------------------------------
    // Import dedupe
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("import dedupe")
    class ImportDedupe {

        @BeforeEach
        void setUpRunner() {
            runner = new StravaImportRunner(
                    service,
                    connectionRepository,
                    importedRepository,
                    activityService,
                    new com.strava.backend.service.impl.StravaRateLimiter(),
                    new ObjectMapper());
            ReflectionTestUtils.setField(runner, "restTemplate", restTemplate);
        }

        @Test
        @DisplayName("skips Strava activities that already exist in the ledger")
        void skipsAlreadyImported() throws Exception {
            // Stub a valid token so refreshTokenIfNeeded short-circuits.
            StravaConnection conn = StravaConnection.builder()
                    .accessToken("t")
                    .refreshToken("r")
                    .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            when(connectionRepository.findByUserId(1L)).thenReturn(Optional.of(conn));

            // Existing ledger says id=111 already imported; 222 is new.
            when(importedRepository.findStravaActivityIdsByUserId(1L)).thenReturn(List.of(111L));
            when(importedRepository.existsByStravaActivityIdAndUserId(222L, 1L)).thenReturn(false);

            // First page returns two activities; next page empty.
            String page1 = """
                    [
                      {
                        "id": 111,
                        "name": "Already imported",
                        "sport_type": "Run",
                        "distance": 5000,
                        "moving_time": 1800,
                        "elapsed_time": 1800,
                        "start_date_local": "2026-04-01T06:30:00Z"
                      },
                      {
                        "id": 222,
                        "name": "New run",
                        "sport_type": "Run",
                        "distance": 10000,
                        "moving_time": 3600,
                        "elapsed_time": 3600,
                        "start_date_local": "2026-04-02T07:00:00Z"
                      }
                    ]
                    """;
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(page1, HttpStatus.OK));

            Activity created = Activity.builder().id(9001L).build();
            when(activityService.createActivity(eq(1L), any(ActivityRequest.class))).thenReturn(created);
            when(connectionRepository.save(any(StravaConnection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            runner.run(1L);

            // Only the second activity (id=222) should have produced a createActivity call.
            verify(activityService, times(1)).createActivity(eq(1L), any(ActivityRequest.class));
            ArgumentCaptor<StravaImportedActivity> savedRec =
                    ArgumentCaptor.forClass(StravaImportedActivity.class);
            verify(importedRepository, times(1)).save(savedRec.capture());
            assertThat(savedRec.getValue().getStravaActivityId()).isEqualTo(222L);
            assertThat(savedRec.getValue().getUserId()).isEqualTo(1L);
            assertThat(savedRec.getValue().getActivityId()).isEqualTo(9001L);
        }

        @Test
        @DisplayName("converts Strava activity JSON to ActivityRequest with metric-normalised fields")
        void mapsActivityJsonCorrectly() throws Exception {
            String json = """
                    {
                      "id": 5,
                      "name": "Morning Run",
                      "sport_type": "Run",
                      "distance": 5000,
                      "moving_time": 1500,
                      "elapsed_time": 1600,
                      "total_elevation_gain": 42,
                      "average_speed": 3.33,
                      "max_speed": 4.0,
                      "average_heartrate": 150,
                      "max_heartrate": 175,
                      "start_date_local": "2026-04-01T06:30:00Z",
                      "private": true
                    }
                    """;
            com.fasterxml.jackson.databind.JsonNode node = new ObjectMapper().readTree(json);

            ActivityRequest req = service.toActivityRequest(node);

            assertThat(req.getTitle()).isEqualTo("Morning Run");
            // 5000m -> 5.0km
            assertThat(req.getDistance()).isEqualTo(5.0);
            // Prefer moving_time over elapsed_time.
            assertThat(req.getDuration()).isEqualTo(1500);
            assertThat(req.getElevationGain()).isEqualTo(42);
            // 3.33 m/s -> 11.988 km/h
            assertThat(req.getAverageSpeed()).isEqualTo(3.33 * 3.6);
            assertThat(req.getAverageHeartRate()).isEqualTo(150);
            assertThat(req.isPrivate()).isTrue();
            assertThat(req.getActivityType()).isNotNull();
        }
    }
}
