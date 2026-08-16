package com.strava.activityservice.gear;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.activityservice.config.SecurityConfig;
import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.Gear;
import com.strava.activityservice.domain.GearType;
import com.strava.activityservice.exception.ApiRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;
import java.util.List;

import static com.strava.activityservice.constants.ErrorMessage.GEAR_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GearController.class)
@Import({SecurityConfig.class, GearMapper.class})
@DisplayName("GearController Unit Tests")
class GearControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GearService gearService;

    private ObjectMapper objectMapper;
    private Gear gear;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gear = Gear.builder()
                .id(10L)
                .userId(1L)
                .name("Saucony Endorphin Pro 3")
                .brand("Saucony")
                .type(GearType.SHOES)
                .activityTypes(EnumSet.of(ActivityType.RUN))
                .distanceKmTotal(42.5)
                .activityCount(4)
                .retired(false)
                .build();
    }

    private GearRequest request() {
        GearRequest request = new GearRequest();
        request.setName("Saucony Endorphin Pro 3");
        request.setType(GearType.SHOES);
        return request;
    }

    @Test
    @DisplayName("POST /api/v1/gear returns 201 with the mapped response")
    void createReturns201() throws Exception {
        when(gearService.createGear(eq(1L), any(GearRequest.class))).thenReturn(gear);

        mockMvc.perform(post("/api/v1/gear")
                        .header(USER_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Saucony Endorphin Pro 3"))
                .andExpect(jsonPath("$.type").value("SHOES"))
                .andExpect(jsonPath("$.activityTypes[0]").value("RUN"))
                // Formatting mirrors main's default-locale String.format — compute
                // the expectation the same way so the test is locale-agnostic.
                .andExpect(jsonPath("$.distanceFormatted").value(String.format("%.2f km", 42.5)));
    }

    @Test
    @DisplayName("POST /api/v1/gear rejects a blank name with the contract message")
    void createRejectsBlankName() throws Exception {
        GearRequest bad = request();
        bad.setName("  ");

        mockMvc.perform(post("/api/v1/gear")
                        .header(USER_HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gear name cannot be empty"));
    }

    @Test
    @DisplayName("GET /api/v1/gear defaults includeRetired to false")
    void listDefaultsToActiveOnly() throws Exception {
        when(gearService.getUserGear(1L, false)).thenReturn(List.of(gear));

        mockMvc.perform(get("/api/v1/gear").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));

        verify(gearService).getUserGear(1L, false);
    }

    @Test
    @DisplayName("GET /api/v1/gear?includeRetired=true passes the flag through")
    void listIncludesRetiredWhenAsked() throws Exception {
        when(gearService.getUserGear(1L, true)).thenReturn(List.of(gear));

        mockMvc.perform(get("/api/v1/gear").param("includeRetired", "true").header(USER_HEADER, "1"))
                .andExpect(status().isOk());

        verify(gearService).getUserGear(1L, true);
    }

    @Test
    @DisplayName("GET /api/v1/gear/{id} surfaces the ProblemDetail message on 404")
    void get404CarriesMessage() throws Exception {
        when(gearService.getGearById(99L, 1L))
                .thenThrow(new ApiRequestException(GEAR_NOT_FOUND, HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/gear/99").header(USER_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(GEAR_NOT_FOUND));
    }

    @Test
    @DisplayName("DELETE /api/v1/gear/{id} returns 204")
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/gear/10").header(USER_HEADER, "1"))
                .andExpect(status().isNoContent());

        verify(gearService).deleteGear(10L, 1L);
    }

    @Test
    @DisplayName("POST /api/v1/gear/{id}/retire returns the retired gear")
    void retireReturnsGear() throws Exception {
        gear.setRetired(true);
        when(gearService.retireGear(10L, 1L)).thenReturn(gear);

        mockMvc.perform(post("/api/v1/gear/10/retire").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retired").value(true));
    }

    @Test
    @DisplayName("requests without gateway identity headers are rejected")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/gear"))
                .andExpect(status().isForbidden());
    }
}
