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
            // Disable streams by default in the dedupe test — the existing assertions only
            // care about the activity-list flow; streams-specific coverage lives in
            // ImportStreams below.
            stravaProperties.setImportStreams(false);
            runner = new StravaImportRunner(
                    service,
                    connectionRepository,
                    importedRepository,
                    activityService,
                    new com.strava.backend.service.impl.StravaRateLimiter(),
                    new ObjectMapper(),
                    stravaProperties);
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

    // ---------------------------------------------------------------------
    // Streams import
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("import streams")
    class ImportStreams {

        @BeforeEach
        void setUpRunner() {
            stravaProperties.setImportStreams(true);
            runner = new StravaImportRunner(
                    service,
                    connectionRepository,
                    importedRepository,
                    activityService,
                    new com.strava.backend.service.impl.StravaRateLimiter(),
                    new ObjectMapper(),
                    stravaProperties);
            ReflectionTestUtils.setField(runner, "restTemplate", restTemplate);
        }

        @Test
        @DisplayName("persists polyline + timestamps + elevation from the streams endpoint")
        void persistsStreamsOnSuccess() throws Exception {
            // Stub a valid token so refreshTokenIfNeeded short-circuits.
            StravaConnection conn = StravaConnection.builder()
                    .accessToken("t")
                    .refreshToken("r")
                    .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            when(connectionRepository.findByUserId(1L)).thenReturn(Optional.of(conn));
            when(importedRepository.findStravaActivityIdsByUserId(1L)).thenReturn(List.of());
            when(importedRepository.existsByStravaActivityIdAndUserId(777L, 1L)).thenReturn(false);

            // Activity list page.
            String page = """
                    [
                      {
                        "id": 777,
                        "name": "GPS run",
                        "sport_type": "Run",
                        "distance": 3000,
                        "moving_time": 900,
                        "elapsed_time": 900,
                        "start_date_local": "2026-04-01T06:30:00Z"
                      }
                    ]
                    """;
            // Streams response aligned index-for-index: 3 points.
            String streams = """
                    {
                      "latlng":    { "data": [[52.22, 21.00], [52.23, 21.01], [52.24, 21.02]] },
                      "time":      { "data": [0, 60, 120] },
                      "altitude":  { "data": [100.0, 105.5, 110.0] },
                      "heartrate": { "data": [120, 130, 140] }
                    }
                    """;

            // Route requests by URL so the page and streams calls get the right payload.
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0);
                        if (url.contains("/activities/777/streams")) {
                            return new ResponseEntity<>(streams, HttpStatus.OK);
                        }
                        return new ResponseEntity<>(page, HttpStatus.OK);
                    });

            Activity created = Activity.builder().id(9001L).build();
            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            when(activityService.createActivity(eq(1L), reqCaptor.capture())).thenReturn(created);
            when(connectionRepository.save(any(StravaConnection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            runner.run(1L);

            // Exactly one streams call per imported activity.
            verify(restTemplate, times(1)).exchange(
                    org.mockito.ArgumentMatchers.contains("/activities/777/streams"),
                    eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));

            ActivityRequest persisted = reqCaptor.getValue();
            assertThat(persisted.getRoutePolyline())
                    .as("latlng.data must be encoded as a Google polyline")
                    .isNotBlank();
            // Decode it back and verify the coordinates round-trip.
            List<double[]> decoded = com.strava.backend.service.importer.GeoUtils
                    .decodePolyline(persisted.getRoutePolyline());
            assertThat(decoded).hasSize(3);
            assertThat(decoded.get(0)[0]).isCloseTo(52.22, org.assertj.core.data.Offset.offset(1e-4));
            assertThat(decoded.get(0)[1]).isCloseTo(21.00, org.assertj.core.data.Offset.offset(1e-4));

            // pointTimestamps: start_date_local "2026-04-01T06:30:00Z" is parsed as UTC offset
            // by toActivityRequest, giving a base epoch of 1_775_011_800. Offsets are
            // [0, 60, 120] → absolute epoch seconds [base, base+60, base+120].
            long expectedBase = java.time.ZonedDateTime
                    .parse("2026-04-01T06:30:00Z")
                    .toEpochSecond();
            long[] ts = new ObjectMapper().readValue(persisted.getPointTimestamps(), long[].class);
            assertThat(ts).containsExactly(expectedBase, expectedBase + 60, expectedBase + 120);

            double[] elev = new ObjectMapper().readValue(persisted.getElevationProfile(), double[].class);
            assertThat(elev).containsExactly(100.0, 105.5, 110.0);
        }

        @Test
        @DisplayName("still creates the Activity with null GPS fields when streams returns 404")
        void tolerates404FromStreams() throws Exception {
            StravaConnection conn = StravaConnection.builder()
                    .accessToken("t")
                    .refreshToken("r")
                    .tokenExpiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            when(connectionRepository.findByUserId(1L)).thenReturn(Optional.of(conn));
            when(importedRepository.findStravaActivityIdsByUserId(1L)).thenReturn(List.of());
            when(importedRepository.existsByStravaActivityIdAndUserId(888L, 1L)).thenReturn(false);

            String page = """
                    [
                      {
                        "id": 888,
                        "name": "Treadmill run (no GPS)",
                        "sport_type": "Run",
                        "distance": 5000,
                        "moving_time": 1800,
                        "elapsed_time": 1800,
                        "start_date_local": "2026-04-02T07:00:00Z"
                      }
                    ]
                    """;

            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenAnswer(inv -> {
                        String url = inv.getArgument(0);
                        if (url.contains("/activities/888/streams")) {
                            // Manual activities and non-GPS indoor sessions return 404 from
                            // the streams endpoint — must not abort the whole import.
                            throw org.springframework.web.client.HttpClientErrorException.create(
                                    HttpStatus.NOT_FOUND,
                                    "Not Found",
                                    new org.springframework.http.HttpHeaders(),
                                    new byte[0],
                                    null);
                        }
                        return new ResponseEntity<>(page, HttpStatus.OK);
                    });

            Activity created = Activity.builder().id(9002L).build();
            ArgumentCaptor<ActivityRequest> reqCaptor = ArgumentCaptor.forClass(ActivityRequest.class);
            when(activityService.createActivity(eq(1L), reqCaptor.capture())).thenReturn(created);
            when(connectionRepository.save(any(StravaConnection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            runner.run(1L);

            // The activity must still be created — summary row, no GPS.
            verify(activityService, times(1)).createActivity(eq(1L), any(ActivityRequest.class));
            verify(importedRepository, times(1)).save(any(StravaImportedActivity.class));

            ActivityRequest persisted = reqCaptor.getValue();
            assertThat(persisted.getPointTimestamps()).isNull();
            assertThat(persisted.getElevationProfile()).isNull();
            // routePolyline stays whatever the summary had (null in this test since no "map" field).
            assertThat(persisted.getRoutePolyline()).isNull();
        }
    }
}
