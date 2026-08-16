package com.strava.socialservice.feed;

import com.strava.socialservice.common.HeaderResponse;
import com.strava.socialservice.config.SecurityConfig;
import com.strava.socialservice.user.BaseUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@Import(SecurityConfig.class)
@DisplayName("FeedController")
class FeedControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private FeedQueryService feedQueryService;

    private ActivityResponse row;

    @BeforeEach
    void setUp() {
        BaseUserResponse author = new BaseUserResponse();
        author.setId(1L);
        author.setFirstName("Pan");
        author.setLastName("Szczesniak");

        row = new ActivityResponse();
        row.setId(6L);
        row.setTitle("Park Run");
        row.setActivityType(ActivityType.RUN);
        row.setDistance(5.0);
        row.setDuration(1500);
        row.setPrivate(false);
        row.setLikesCount(2);
        row.setCommentsCount(1);
        row.setLikedByCurrentUser(true);
        row.setCreatedAt(LocalDateTime.of(2026, 7, 1, 8, 0));
        row.setUser(author);
    }

    @Test
    @DisplayName("requests without gateway identity headers are rejected")
    void unauthenticatedRejected() throws Exception {
        mockMvc.perform(get("/api/v1/activities/feed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /feed replies with main's envelope: plain array + page headers, privacy flag as 'private', derived fullName")
    void feedEnvelope() throws Exception {
        when(feedQueryService.getFeed(eq(1L), any(Pageable.class)))
                .thenReturn(HeaderResponse.of(List.of(row), 3, 25));

        mockMvc.perform(get("/api/v1/activities/feed").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("page-total-count", "3"))
                .andExpect(header().string("page-total-elements", "25"))
                .andExpect(jsonPath("$[0].id").value(6))
                .andExpect(jsonPath("$[0].title").value("Park Run"))
                .andExpect(jsonPath("$[0].activityType").value("RUN"))
                .andExpect(jsonPath("$[0].likesCount").value(2))
                .andExpect(jsonPath("$[0].commentsCount").value(1))
                .andExpect(jsonPath("$[0].likedByCurrentUser").value(true))
                // main's Lombok/Jackson quirk: boolean isPrivate serializes as "private"
                .andExpect(jsonPath("$[0].private").value(false))
                .andExpect(jsonPath("$[0].isPrivate").doesNotExist())
                .andExpect(jsonPath("$[0].user.id").value(1))
                .andExpect(jsonPath("$[0].user.fullName").value("Pan Szczesniak"))
                // contract fields that are always present (null) on social-served rows
                .andExpect(jsonPath("$[0].gear").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("GET /feed defaults to page size 10")
    void feedDefaultPageSize() throws Exception {
        when(feedQueryService.getFeed(eq(1L), any(Pageable.class)))
                .thenReturn(HeaderResponse.of(List.of(), 0, 0));

        mockMvc.perform(get("/api/v1/activities/feed").header(USER_HEADER, "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedQueryService).getFeed(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    @DisplayName("GET /public forwards page/size and returns the same envelope")
    void publicEnvelope() throws Exception {
        when(feedQueryService.getPublicActivities(eq(1L), any(Pageable.class)))
                .thenReturn(HeaderResponse.of(List.of(row), 1, 1));

        mockMvc.perform(get("/api/v1/activities/public?page=2&size=5").header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("page-total-count", "1"))
                .andExpect(header().string("page-total-elements", "1"))
                .andExpect(jsonPath("$[0].id").value(6));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(feedQueryService).getPublicActivities(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("the pre-parity /api/feed surface is gone")
    void oldSurfaceGone() throws Exception {
        mockMvc.perform(get("/api/feed").header(USER_HEADER, "1"))
                .andExpect(status().isNotFound());
    }
}
