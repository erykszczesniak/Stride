package com.strava.socialservice.internal;

import com.strava.socialservice.config.SecurityConfig;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalSocialController.class)
@Import({SecurityConfig.class, SocialSummaryService.class})
@DisplayName("InternalSocialController")
class InternalSocialControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ActivityLikeRepository likeRepository;
    @MockitoBean private CommentRepository commentRepository;

    @Test
    @DisplayName("is reachable without identity headers (cluster-internal, permitAll)")
    void permitsServiceToServiceCalls() throws Exception {
        when(likeRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        when(commentRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());

        mockMvc.perform(get("/internal/social/activities?ids=1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns the exact field names SocialSummaryClient parses, zero-filling unknown ids")
    void summaryContractShape() throws Exception {
        when(likeRepository.countByActivityIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(commentRepository.countByActivityIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        when(likeRepository.findActivityIdsLikedBy(eq(9L), anyCollection()))
                .thenReturn(List.of(1L));

        mockMvc.perform(get("/internal/social/activities?ids=1,2&viewerId=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityId").value(1))
                .andExpect(jsonPath("$[0].likesCount").value(3))
                .andExpect(jsonPath("$[0].commentsCount").value(2))
                .andExpect(jsonPath("$[0].likedByViewer").value(true))
                .andExpect(jsonPath("$[1].activityId").value(2))
                .andExpect(jsonPath("$[1].likesCount").value(0))
                .andExpect(jsonPath("$[1].commentsCount").value(0))
                .andExpect(jsonPath("$[1].likedByViewer").value(false));
    }

    @Test
    @DisplayName("without viewerId no likedByViewer lookup happens and everything is false")
    void withoutViewer() throws Exception {
        when(likeRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        when(commentRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());

        mockMvc.perform(get("/internal/social/activities?ids=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityId").value(5))
                .andExpect(jsonPath("$[0].likedByViewer").value(false));

        org.mockito.Mockito.verify(likeRepository, org.mockito.Mockito.never())
                .findActivityIdsLikedBy(org.mockito.ArgumentMatchers.anyLong(), anyCollection());
    }
}
