package com.strava.socialservice.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.socialservice.config.SecurityConfig;
import com.strava.socialservice.exception.ApiRequestException;
import com.strava.socialservice.user.BaseUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.strava.socialservice.constants.ErrorMessage.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
@DisplayName("CommentController")
class CommentControllerTest {

    private static final String USER_HEADER = "X-User-Id";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CommentService commentService;

    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        BaseUserResponse author = new BaseUserResponse();
        author.setId(1L);
        author.setFirstName("Pan");
        author.setLastName("Szczesniak");

        commentResponse = new CommentResponse();
        commentResponse.setId(11L);
        commentResponse.setContent("Great pace!");
        commentResponse.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        commentResponse.setUser(author);
    }

    @Nested
    @DisplayName("Security")
    class Security {

        @Test
        @DisplayName("requests without gateway identity headers are rejected")
        void unauthenticatedRejected() throws Exception {
            mockMvc.perform(get("/api/v1/activities/7/comments"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/activities/{activityId}/comments")
    class ListComments {

        @Test
        @DisplayName("returns the comment list with embedded author incl. derived fullName")
        void listsComments() throws Exception {
            when(commentService.getCommentsByActivityId(7L)).thenReturn(List.of(commentResponse));

            mockMvc.perform(get("/api/v1/activities/7/comments").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(11))
                    .andExpect(jsonPath("$[0].content").value("Great pace!"))
                    .andExpect(jsonPath("$[0].user.id").value(1))
                    .andExpect(jsonPath("$[0].user.firstName").value("Pan"))
                    .andExpect(jsonPath("$[0].user.fullName").value("Pan Szczesniak"));
        }

        @Test
        @DisplayName("unknown activity surfaces a 404 ProblemDetail with a message property")
        void unknownActivity404s() throws Exception {
            when(commentService.getCommentsByActivityId(7L))
                    .thenThrow(new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/activities/7/comments").header(USER_HEADER, "1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(ACTIVITY_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities/{activityId}/comments")
    class CreateComment {

        @Test
        @DisplayName("201s with the CommentResponse body")
        void creates() throws Exception {
            when(commentService.createComment(7L, 1L, "Great pace!")).thenReturn(commentResponse);

            mockMvc.perform(post("/api/v1/activities/7/comments")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "Great pace!"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(11))
                    .andExpect(jsonPath("$.content").value("Great pace!"));
        }

        @Test
        @DisplayName("blank content is a 400 with main's validation text")
        void blankContent400s() throws Exception {
            mockMvc.perform(post("/api/v1/activities/7/comments")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "  "))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(EMPTY_COMMENT))
                    .andExpect(jsonPath("$.fieldErrors.content").value(EMPTY_COMMENT));
        }
    }

    @Nested
    @DisplayName("PUT + DELETE /api/v1/activities/{activityId}/comments/{commentId}")
    class UpdateDelete {

        @Test
        @DisplayName("PUT returns the updated comment")
        void updates() throws Exception {
            when(commentService.updateComment(11L, 1L, "edited")).thenReturn(commentResponse);

            mockMvc.perform(put("/api/v1/activities/7/comments/11")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "edited"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(11));
        }

        @Test
        @DisplayName("non-author PUT surfaces main's 403")
        void nonAuthorPut403s() throws Exception {
            when(commentService.updateComment(11L, 2L, "edited"))
                    .thenThrow(new ApiRequestException(ACCESS_DENIED, HttpStatus.FORBIDDEN));

            mockMvc.perform(put("/api/v1/activities/7/comments/11")
                            .header(USER_HEADER, "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("content", "edited"))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(ACCESS_DENIED));
        }

        @Test
        @DisplayName("DELETE is a 204 with no body")
        void deletes() throws Exception {
            mockMvc.perform(delete("/api/v1/activities/7/comments/11").header(USER_HEADER, "1"))
                    .andExpect(status().isNoContent());

            verify(commentService).deleteComment(11L, 1L);
        }
    }

    @Nested
    @DisplayName("reactions endpoints")
    class Reactions {

        @Test
        @DisplayName("POST reactions replies with main's message/reactions/userReactions map")
        void addReaction() throws Exception {
            when(commentService.getReactionCounts(11L)).thenReturn(Map.of("🔥", 2L));
            when(commentService.getUserReactions(11L, 1L)).thenReturn(List.of("🔥"));

            mockMvc.perform(post("/api/v1/activities/7/comments/11/reactions")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("emoji", "🔥"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Reaction added"))
                    .andExpect(jsonPath("$.reactions.['🔥']").value(2))
                    .andExpect(jsonPath("$.userReactions[0]").value("🔥"));

            verify(commentService).addReaction(11L, 1L, "🔥");
        }

        @Test
        @DisplayName("DELETE reactions replies with 'Reaction removed' and the refreshed maps")
        void removeReaction() throws Exception {
            when(commentService.getReactionCounts(11L)).thenReturn(Map.of());
            when(commentService.getUserReactions(11L, 1L)).thenReturn(List.of());

            mockMvc.perform(delete("/api/v1/activities/7/comments/11/reactions")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("emoji", "🔥"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Reaction removed"))
                    .andExpect(jsonPath("$.reactions").isEmpty())
                    .andExpect(jsonPath("$.userReactions").isEmpty());

            verify(commentService).removeReaction(11L, 1L, "🔥");
        }

        @Test
        @DisplayName("GET reactions returns main's exact map shape (no message key)")
        void getReactions() throws Exception {
            when(commentService.getReactionCounts(11L)).thenReturn(Map.of("👍", 1L));
            when(commentService.getUserReactions(11L, 1L)).thenReturn(List.of("👍"));

            mockMvc.perform(get("/api/v1/activities/7/comments/11/reactions").header(USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").doesNotExist())
                    .andExpect(jsonPath("$.reactions.['👍']").value(1))
                    .andExpect(jsonPath("$.userReactions[0]").value("👍"));
        }

        @Test
        @DisplayName("missing emoji is a 400 with the validation message")
        void missingEmoji400s() throws Exception {
            mockMvc.perform(post("/api/v1/activities/7/comments/11/reactions")
                            .header(USER_HEADER, "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Emoji is required"));
        }
    }

    @Nested
    @DisplayName("legacy endpoints")
    class Legacy {

        @Test
        @DisplayName("the pre-parity /api/comments/** surface is gone")
        void oldSurfaceGone() throws Exception {
            when(commentService.getCommentsByActivityId(any())).thenReturn(List.of());
            mockMvc.perform(get("/api/comments/activity/7").header(USER_HEADER, "1"))
                    .andExpect(status().isNotFound());
        }
    }
}
