package com.strava.activityservice.strava;

import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.StravaConnection;
import com.strava.activityservice.exception.ApiRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StravaIntegrationController.class)
@Import({SecurityConfig.class, StravaProperties.class})
@DisplayName("StravaIntegrationController Unit Tests")
class StravaIntegrationControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StravaIntegrationService stravaService;

    @Nested
    @DisplayName("authorize")
    class Authorize {

        @Test
        @DisplayName("returns the authorize URL as JSON by default")
        void returnsUrlJson() throws Exception {
            when(stravaService.buildAuthorizeUrl(1L)).thenReturn("https://www.strava.com/oauth/authorize?x=1");

            mockMvc.perform(get("/api/v1/integrations/strava/authorize").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://www.strava.com/oauth/authorize?x=1"));
        }

        @Test
        @DisplayName("302s straight to Strava when ?redirect=true")
        void redirectsWhenAsked() throws Exception {
            when(stravaService.buildAuthorizeUrl(1L)).thenReturn("https://www.strava.com/oauth/authorize?x=1");

            mockMvc.perform(get("/api/v1/integrations/strava/authorize")
                            .param("redirect", "true")
                            .header(USER_HEADER, "1"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "https://www.strava.com/oauth/authorize?x=1"));
        }

        @Test
        @DisplayName("propagates 503 when the integration is unconfigured")
        void unconfiguredIs503() throws Exception {
            when(stravaService.buildAuthorizeUrl(1L)).thenThrow(new ApiRequestException(
                    "Strava integration is not configured on this server",
                    HttpStatus.SERVICE_UNAVAILABLE));

            mockMvc.perform(get("/api/v1/integrations/strava/authorize").header(USER_HEADER, "1"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message")
                            .value("Strava integration is not configured on this server"));
        }

        @Test
        @DisplayName("requires gateway identity headers")
        void requiresAuth() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/strava/authorize"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("callback (permitAll — Strava redirects the browser here without headers)")
    class Callback {

        @Test
        @DisplayName("is reachable WITHOUT identity headers and redirects with stravaConnected=true")
        void successRedirect() throws Exception {
            when(stravaService.verifyStateAndExtractUserId("signed-state")).thenReturn(7L);
            when(stravaService.exchangeCodeAndStoreConnection(7L, "the-code"))
                    .thenReturn(StravaConnection.builder().userId(7L).build());

            mockMvc.perform(get("/api/v1/integrations/strava/callback")
                            .param("code", "the-code")
                            .param("state", "signed-state"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(
                            "http://localhost:3000/settings?section=account&stravaConnected=true"));
        }

        @Test
        @DisplayName("bounces to the frontend with stravaError when Strava reports an error")
        void errorParamRedirect() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/strava/callback")
                            .param("error", "access_denied"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(
                            "http://localhost:3000/settings?section=account&stravaError=access_denied"));

            verify(stravaService, never()).exchangeCodeAndStoreConnection(any(), any());
        }

        @Test
        @DisplayName("bounces with stravaError=connection_failed when the state is invalid")
        void badStateRedirect() throws Exception {
            when(stravaService.verifyStateAndExtractUserId("bad-state"))
                    .thenThrow(new ApiRequestException("Invalid Strava OAuth state signature",
                            HttpStatus.BAD_REQUEST));

            mockMvc.perform(get("/api/v1/integrations/strava/callback")
                            .param("code", "c")
                            .param("state", "bad-state"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(
                            "http://localhost:3000/settings?section=account&stravaError=connection_failed"));
        }
    }

    @Nested
    @DisplayName("import / status / disconnect")
    class ImportStatusDisconnect {

        @Test
        @DisplayName("POST /import answers 202 with the queued message")
        void importAccepted() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/strava/import").header(USER_HEADER, "1"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.started").value(true))
                    .andExpect(jsonPath("$.message").value("Strava import queued — check back shortly."));

            verify(stravaService).startImportForUser(1L);
        }

        @Test
        @DisplayName("GET /status returns the connection snapshot")
        void statusReturnsSnapshot() throws Exception {
            when(stravaService.getStatus(1L)).thenReturn(StravaStatusResponse.builder()
                    .connected(true)
                    .athleteId(42L)
                    .connectedAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                    .activityCount(17)
                    .build());

            mockMvc.perform(get("/api/v1/integrations/strava/status").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.connected").value(true))
                    .andExpect(jsonPath("$.athleteId").value(42))
                    .andExpect(jsonPath("$.activityCount").value(17));
        }

        @Test
        @DisplayName("DELETE /disconnect answers 204")
        void disconnectNoContent() throws Exception {
            mockMvc.perform(delete("/api/v1/integrations/strava/disconnect").header(USER_HEADER, "1"))
                    .andExpect(status().isNoContent());

            verify(stravaService).disconnect(1L);
        }

        @Test
        @DisplayName("import/status/disconnect all require gateway identity headers")
        void requireAuth() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/strava/import"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/integrations/strava/status"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/integrations/strava/disconnect"))
                    .andExpect(status().isForbidden());
        }
    }
}
