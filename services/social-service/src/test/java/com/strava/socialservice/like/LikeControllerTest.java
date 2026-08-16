package com.strava.socialservice.like;

import com.strava.socialservice.config.SecurityConfig;
import com.strava.socialservice.exception.ApiRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.strava.socialservice.constants.ErrorMessage.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LikeController.class)
@Import(SecurityConfig.class)
@DisplayName("LikeController")
class LikeControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LikeService likeService;

    @Test
    @DisplayName("requests without gateway identity headers are rejected")
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(post("/api/v1/activities/7/like"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST like replies with main's exact body")
    void like() throws Exception {
        mockMvc.perform(post("/api/v1/activities/7/like").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activity liked"));

        verify(likeService).likeActivity(7L, 1L);
    }

    @Test
    @DisplayName("DELETE like replies with main's exact body")
    void unlike() throws Exception {
        mockMvc.perform(delete("/api/v1/activities/7/like").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activity unliked"));

        verify(likeService).unlikeActivity(7L, 1L);
    }

    @Test
    @DisplayName("double like surfaces main's 400 text as a ProblemDetail message")
    void doubleLike400s() throws Exception {
        doThrow(new ApiRequestException(ACTIVITY_ALREADY_LIKED, HttpStatus.BAD_REQUEST))
                .when(likeService).likeActivity(7L, 1L);

        mockMvc.perform(post("/api/v1/activities/7/like").header(USER_HEADER, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ACTIVITY_ALREADY_LIKED));
    }

    @Test
    @DisplayName("unknown activity surfaces a 404 with main's text")
    void unknownActivity404s() throws Exception {
        doThrow(new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND))
                .when(likeService).unlikeActivity(7L, 1L);

        mockMvc.perform(delete("/api/v1/activities/7/like").header(USER_HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ACTIVITY_NOT_FOUND));
    }
}
