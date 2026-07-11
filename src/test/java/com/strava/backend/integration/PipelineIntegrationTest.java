package com.strava.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PipelineTokenRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.pipeline.PipelineTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the pipeline facade over the real security chains:
 * token issuance, Bearer auth on /pipeline/v1/**, chain separation from
 * /api/v1, and GPS-stream-derived splits in the detail payload.
 */
@DisplayName("Pipeline API Integration Tests")
class PipelineIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private PipelineTokenRepository pipelineTokenRepository;

    @Autowired
    private PipelineTokenService pipelineTokenService;

    @Autowired
    private JwtProvider jwtProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user;
    private Activity run;
    private String token;

    @BeforeEach
    void setUp() {
        pipelineTokenRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("pipeline@test.dev")
                .firstName("Pipe")
                .lastName("Line")
                .password("irrelevant")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .active(true)
                .build());

        // Straight-north track, ~100.07 m per segment, 21 points ≈ 2 km.
        // First km ridden in 300 s, second in 360 s.
        StringBuilder polyline = new StringBuilder("[");
        StringBuilder timestamps = new StringBuilder("[");
        long t = 1_700_000_000L;
        for (int i = 0; i < 21; i++) {
            if (i > 0) {
                polyline.append(",");
                timestamps.append(",");
                t += i <= 10 ? 30 : 36;
            }
            polyline.append("[").append(52.0 + i * 0.0009).append(",21.0]");
            timestamps.append(t);
        }
        polyline.append("]");
        timestamps.append("]");

        Activity activity = Activity.builder()
                .title("Two varied kilometres")
                .activityType(ActivityType.RUN)
                .distance(2.0)
                .duration(660)
                .startTime(LocalDateTime.now().minusHours(3))
                .routePolyline(polyline.toString())
                .pointTimestamps(timestamps.toString())
                .user(user)
                .build();
        activity.setAverageHeartRate(150);
        activity.setMaxHeartRate(165);
        run = activityRepository.save(activity);

        token = pipelineTokenService.issueToken(user.getId(), "integration").plaintext();
    }

    @AfterEach
    void tearDown() {
        pipelineTokenRepository.deleteAll();
        activityRepository.deleteAll();
        userRepository.deleteAll();
    }

    private ResponseEntity<String> getWithToken(String path, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    @DisplayName("rejects missing and unknown tokens with 401")
    void rejectsBadTokens() {
        assertThat(getWithToken("/pipeline/v1/activities", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithToken("/pipeline/v1/activities", "tri_definitely_not_issued").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a pipeline token does not unlock the JWT-protected /api/v1 chain")
    void pipelineTokenDoesNotWorkOnApiChain() {
        assertThat(getWithToken("/api/v1/users/me", token).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a regular Stride JWT is accepted on the pipeline chain")
    void jwtWorksOnPipelineChain() {
        String jwt = jwtProvider.generateToken(user.getId(), user.getEmail());

        assertThat(getWithToken("/pipeline/v1/dashboard", jwt).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("lists the owner's activities in the contract shape")
    void listsActivities() throws Exception {
        ResponseEntity<String> response = getWithToken("/pipeline/v1/activities", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.isArray()).isTrue();
        assertThat(body).hasSize(1);
        JsonNode activity = body.get(0);
        assertThat(activity.get("id").asText()).isEqualTo("act-" + run.getId());
        assertThat(activity.get("discipline").asText()).isEqualTo("run");
        assertThat(activity.get("hasGPS").asBoolean()).isTrue();
        // ISO-8601 with offset — the client's .iso8601 decoder needs it.
        assertThat(activity.get("startDate").asText()).matches(".*([+-]\\d{2}:\\d{2}|Z)$");
    }

    @Test
    @DisplayName("activity detail carries real per-split durations derived from GPS timestamps")
    void detailHasAccurateSplits() throws Exception {
        ResponseEntity<String> response =
                getWithToken("/pipeline/v1/activities/act-" + run.getId(), token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        JsonNode splits = body.get("splits");
        assertThat(splits).hasSize(2);
        assertThat(splits.get(0).get("distanceMeters").asInt()).isEqualTo(1000);
        assertThat(splits.get(0).get("durationSeconds").asInt()).isEqualTo(300);
        assertThat(splits.get(1).get("durationSeconds").asInt()).isEqualTo(360);

        var durations = new HashSet<Integer>();
        splits.forEach(s -> durations.add(s.get("durationSeconds").asInt()));
        assertThat(durations.size()).isGreaterThan(1); // no more average-pace clones

        assertThat(body.get("paceSeries")).hasSize(17);
        assertThat(body.get("heartRateSeries")).hasSize(17);
    }

    @Test
    @DisplayName("unknown ids return the contract error body")
    void unknownIdReturnsContractError() throws Exception {
        ResponseEntity<String> response = getWithToken("/pipeline/v1/activities/act-999999", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("code").asText()).isEqualTo("not_found");
    }

    @Test
    @DisplayName("dashboard and profile respond with the required top-level fields")
    void dashboardAndProfileShapes() throws Exception {
        JsonNode dashboard = objectMapper.readTree(getWithToken("/pipeline/v1/dashboard", token).getBody());
        assertThat(dashboard.get("form").fieldNames())
                .toIterable().map(String::toString)
                .contains("date", "ctl", "atl", "tsb");
        assertThat(dashboard.has("todayWorkout")).isTrue();
        assertThat(dashboard.get("snapshots")).hasSize(3);

        JsonNode profile = objectMapper.readTree(getWithToken("/pipeline/v1/athlete/profile", token).getBody());
        assertThat(profile.get("athlete").get("initials").asText()).isEqualTo("PL");
        assertThat(profile.get("seasonStats").get("sessions").asInt()).isEqualTo(1);
        assertThat(profile.get("thresholdZones").has("ftpWatts")).isTrue();

        JsonNode load = objectMapper.readTree(
                getWithToken("/pipeline/v1/metrics/load?range=4w", token).getBody());
        assertThat(load.get("range").asText()).isEqualTo("4w");
        assertThat(load.get("ctl").size()).isGreaterThan(3);
        assertThat(load.get("weeklyVolumes")).hasSize(12);
        assertThat(load.get("weekSummary").has("loadTss")).isTrue();

        var revoked = pipelineTokenRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().peek(tok -> tok.setRevoked(true)).collect(Collectors.toList());
        pipelineTokenRepository.saveAll(revoked);
        assertThat(getWithToken("/pipeline/v1/dashboard", token).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
