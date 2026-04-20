package com.strava.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.configuration.StravaProperties;
import com.strava.backend.domain.StravaConnection;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.ActivityRequest;
import com.strava.backend.dto.strava.StravaStatusResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.StravaConnectionRepository;
import com.strava.backend.repository.StravaImportedActivityRepository;
import com.strava.backend.service.StravaIntegrationService;
import com.strava.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class StravaIntegrationServiceImpl implements StravaIntegrationService {

    static final String AUTH_URL = "https://www.strava.com/oauth/authorize";
    static final String TOKEN_URL = "https://www.strava.com/api/v3/oauth/token";
    static final String ACTIVITIES_URL = "https://www.strava.com/api/v3/athlete/activities";
    static final String SCOPES = "read,activity:read_all";

    private final StravaProperties stravaProperties;
    private final StravaStateSigner stateSigner;
    private final StravaConnectionRepository connectionRepository;
    private final StravaImportedActivityRepository importedRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    /**
     * Fetched lazily because {@link StravaImportRunner} depends on this service —
     * using a field injection here would create a cycle at bean-construction time.
     */
    private final ApplicationContext applicationContext;

    /**
     * {@link RestTemplate} is not a bean in this project, so we instantiate one directly.
     * The test uses {@link org.springframework.test.util.ReflectionTestUtils} to swap it.
     */
    private RestTemplate restTemplate = new RestTemplate();

    // ---------------------------------------------------------------------
    // OAuth flow
    // ---------------------------------------------------------------------

    @Override
    public String buildAuthorizeUrl(Long userId) {
        requireConfigured();
        String state = stateSigner.sign(userId);
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", stravaProperties.getClientId())
                .queryParam("redirect_uri", stravaProperties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("approval_prompt", "auto")
                .queryParam("scope", SCOPES)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    @Override
    public Long verifyStateAndExtractUserId(String state) {
        return stateSigner.verify(state);
    }

    @Override
    @Transactional
    public StravaConnection exchangeCodeAndStoreConnection(Long userId, String code) {
        requireConfigured();
        if (code == null || code.isBlank()) {
            throw new ApiRequestException("Missing Strava authorization code", HttpStatus.BAD_REQUEST);
        }

        String body = "client_id=" + urlEncode(stravaProperties.getClientId())
                + "&client_secret=" + urlEncode(stravaProperties.getClientSecret())
                + "&code=" + urlEncode(code)
                + "&grant_type=authorization_code";

        JsonNode tokenJson = postForm(TOKEN_URL, body, "exchange code");

        User user = userService.getUserById(userId);
        StravaConnection connection = connectionRepository.findByUserId(userId)
                .orElseGet(() -> StravaConnection.builder().user(user).build());
        applyTokenResponse(connection, tokenJson);
        StravaConnection saved = connectionRepository.save(connection);
        log.info("Stored Strava connection for user {} (athleteId={})", userId, saved.getStravaAthleteId());
        return saved;
    }

    @Override
    @Transactional
    public String refreshTokenIfNeeded(Long userId) {
        StravaConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiRequestException(
                        "No Strava connection for user", HttpStatus.BAD_REQUEST));

        if (!connection.isTokenExpired() && connection.getAccessToken() != null) {
            return connection.getAccessToken();
        }

        requireConfigured();
        if (connection.getRefreshToken() == null || connection.getRefreshToken().isBlank()) {
            throw new ApiRequestException(
                    "No refresh token available — user must reconnect Strava", HttpStatus.UNAUTHORIZED);
        }

        String body = "client_id=" + urlEncode(stravaProperties.getClientId())
                + "&client_secret=" + urlEncode(stravaProperties.getClientSecret())
                + "&grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(connection.getRefreshToken());

        JsonNode json = postForm(TOKEN_URL, body, "refresh token");
        applyTokenResponse(connection, json);
        connectionRepository.save(connection);
        log.debug("Refreshed Strava access token for user {}", userId);
        return connection.getAccessToken();
    }

    // ---------------------------------------------------------------------
    // Status / disconnect
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StravaStatusResponse getStatus(Long userId) {
        return connectionRepository.findByUserId(userId)
                .map(c -> StravaStatusResponse.builder()
                        .connected(true)
                        .athleteId(c.getStravaAthleteId())
                        .connectedAt(c.getConnectedAt())
                        .lastImportedAt(c.getLastImportedAt())
                        .activityCount(importedRepository.countByUserId(userId))
                        .build())
                .orElseGet(() -> StravaStatusResponse.builder()
                        .connected(false)
                        .activityCount(0)
                        .build());
    }

    @Override
    @Transactional
    public void disconnect(Long userId) {
        importedRepository.deleteByUserId(userId);
        connectionRepository.deleteByUserId(userId);
        log.info("Disconnected Strava for user {}", userId);
    }

    // ---------------------------------------------------------------------
    // Import
    // ---------------------------------------------------------------------

    @Override
    public void startImportForUser(Long userId) {
        if (!connectionRepository.existsByUserId(userId)) {
            throw new ApiRequestException(
                    "Connect Strava before importing", HttpStatus.BAD_REQUEST);
        }
        // Delegate to a separate bean so Spring's @Async proxy is actually invoked.
        applicationContext.getBean(StravaImportRunner.class).run(userId);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private JsonNode postForm(String url, String formBody, String op) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> entity = new HttpEntity<>(formBody, headers);
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("Strava {} failed: {} {}", op, e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiRequestException(
                    "Strava " + op + " failed: " + e.getStatusCode(), HttpStatus.BAD_GATEWAY);
        } catch (Exception e) {
            log.error("Strava {} failed", op, e);
            throw new ApiRequestException("Strava " + op + " failed", HttpStatus.BAD_GATEWAY);
        }
    }

    private void applyTokenResponse(StravaConnection connection, JsonNode json) {
        if (json.hasNonNull("access_token")) {
            connection.setAccessToken(json.get("access_token").asText());
        }
        if (json.hasNonNull("refresh_token")) {
            connection.setRefreshToken(json.get("refresh_token").asText());
        }
        if (json.hasNonNull("expires_at")) {
            long epoch = json.get("expires_at").asLong();
            connection.setTokenExpiresAt(
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault()));
        }
        JsonNode athlete = json.path("athlete");
        if (athlete.hasNonNull("id")) {
            connection.setStravaAthleteId(athlete.get("id").asLong());
        }
        if (json.hasNonNull("scope")) {
            connection.setScope(json.get("scope").asText());
        } else if (connection.getScope() == null) {
            connection.setScope(SCOPES);
        }
    }

    private void requireConfigured() {
        if (!stravaProperties.isConfigured()) {
            throw new ApiRequestException(
                    "Strava integration is not configured on this server",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    /**
     * Convert a Strava activity JSON object to this app's {@link ActivityRequest} DTO.
     * Package-private for testing.
     */
    ActivityRequest toActivityRequest(JsonNode node) {
        ActivityRequest req = new ActivityRequest();
        req.setTitle(text(node, "name", "Imported from Strava"));
        req.setDescription(text(node, "description", null));
        req.setActivityType(mapActivityType(text(node, "sport_type", text(node, "type", null))));

        // Strava distance is meters -> our DB stores kilometers.
        double distanceM = node.path("distance").asDouble(0.0);
        req.setDistance(distanceM > 0 ? distanceM / 1000.0 : 0.001);
        int movingSec = node.path("moving_time").asInt(0);
        int elapsedSec = node.path("elapsed_time").asInt(0);
        req.setDuration(movingSec > 0 ? movingSec : Math.max(elapsedSec, 1));

        if (node.hasNonNull("total_elevation_gain")) {
            req.setElevationGain((int) Math.round(node.get("total_elevation_gain").asDouble()));
        }
        if (node.hasNonNull("calories")) {
            req.setCalories((int) Math.round(node.get("calories").asDouble()));
        }
        if (node.hasNonNull("average_speed")) {
            // Strava m/s -> km/h
            req.setAverageSpeed(node.get("average_speed").asDouble() * 3.6);
        }
        if (node.hasNonNull("max_speed")) {
            req.setMaxSpeed(node.get("max_speed").asDouble() * 3.6);
        }
        if (node.hasNonNull("average_heartrate")) {
            req.setAverageHeartRate((int) Math.round(node.get("average_heartrate").asDouble()));
        }
        if (node.hasNonNull("max_heartrate")) {
            req.setMaxHeartRate((int) Math.round(node.get("max_heartrate").asDouble()));
        }
        if (node.hasNonNull("average_cadence")) {
            req.setAverageCadence((int) Math.round(node.get("average_cadence").asDouble()));
        }
        if (node.hasNonNull("average_watts")) {
            req.setAveragePower((int) Math.round(node.get("average_watts").asDouble()));
        }

        String startStr = text(node, "start_date_local", text(node, "start_date", null));
        if (startStr != null) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(startStr, DateTimeFormatter.ISO_DATE_TIME);
                req.setStartTime(zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
            } catch (Exception parseFail) {
                req.setStartTime(LocalDateTime.now());
            }
        } else {
            req.setStartTime(LocalDateTime.now());
        }

        JsonNode map = node.path("map");
        if (map.hasNonNull("summary_polyline")) {
            req.setRoutePolyline(map.get("summary_polyline").asText());
        }
        req.setPrivate(node.path("private").asBoolean(false));
        return req;
    }

    private static String text(JsonNode node, String field, String fallback) {
        return node.hasNonNull(field) ? node.get(field).asText() : fallback;
    }

    /** Map Strava's {@code sport_type}/{@code type} string to our enum, defaulting to OTHER. */
    static ActivityType mapActivityType(String stravaType) {
        if (stravaType == null) return ActivityType.OTHER;
        String norm = stravaType.trim().toUpperCase().replace(' ', '_');
        switch (norm) {
            case "RUN":
            case "TRAILRUN":
            case "TRAIL_RUN":
                return ActivityType.RUN;
            case "VIRTUALRUN":
            case "VIRTUAL_RUN":
                return ActivityType.VIRTUAL_RUN;
            case "RIDE":
            case "GRAVELRIDE":
            case "GRAVEL_RIDE":
            case "MOUNTAINBIKERIDE":
            case "MOUNTAIN_BIKE_RIDE":
            case "EBIKERIDE":
            case "E_BIKE_RIDE":
                return ActivityType.RIDE;
            case "VIRTUALRIDE":
            case "VIRTUAL_RIDE":
                return ActivityType.VIRTUAL_RIDE;
            case "SWIM":
                return ActivityType.SWIM;
            case "WALK":
                return ActivityType.WALK;
            case "HIKE":
                return ActivityType.HIKE;
            case "ALPINESKI":
            case "ALPINE_SKI":
                return ActivityType.ALPINE_SKI;
            case "BACKCOUNTRYSKI":
            case "BACKCOUNTRY_SKI":
                return ActivityType.BACKCOUNTRY_SKI;
            case "NORDICSKI":
            case "NORDIC_SKI":
                return ActivityType.NORDIC_SKI;
            case "ROLLERSKI":
            case "ROLLER_SKI":
                return ActivityType.ROLLER_SKI;
            case "CROSSFIT":
                return ActivityType.CROSSFIT;
            case "ELLIPTICAL":
                return ActivityType.ELLIPTICAL;
            case "ICESKATE":
            case "ICE_SKATE":
                return ActivityType.ICE_SKATE;
            case "INLINESKATE":
            case "INLINE_SKATE":
                return ActivityType.INLINE_SKATE;
            case "KAYAKING":
                return ActivityType.KAYAKING;
            case "KITESURF":
                return ActivityType.KITESURF;
            case "ROCKCLIMBING":
            case "ROCK_CLIMBING":
                return ActivityType.ROCK_CLIMBING;
            case "ROWING":
                return ActivityType.ROWING;
            case "SNOWBOARD":
                return ActivityType.SNOWBOARD;
            case "SNOWSHOE":
                return ActivityType.SNOWSHOE;
            case "STAIRSTEPPER":
            case "STAIR_STEPPER":
                return ActivityType.STAIR_STEPPER;
            case "STANDUPPADDLING":
            case "STAND_UP_PADDLING":
                return ActivityType.STAND_UP_PADDLING;
            case "SURFING":
                return ActivityType.SURFING;
            case "WEIGHTTRAINING":
            case "WEIGHT_TRAINING":
                return ActivityType.WEIGHT_TRAINING;
            case "WINDSURF":
                return ActivityType.WINDSURF;
            case "WORKOUT":
                return ActivityType.WORKOUT;
            case "YOGA":
                return ActivityType.YOGA;
            default:
                return ActivityType.OTHER;
        }
    }

    // Test seam — lets tests inject a mocked RestTemplate.
    void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
