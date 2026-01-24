package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Comment;
import com.strava.backend.domain.User;
import com.strava.backend.dto.activity.CommentRequest;
import com.strava.backend.dto.activity.CommentResponse;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.CommentService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CommentController Unit Tests")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private Comment testComment;
    private User testUser;
    private Activity testActivity;
    private CommentRequest commentRequest;
    private CommentResponse commentResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testUser = TestDataFactory.createUser();
        testActivity = TestDataFactory.createActivity(1L, testUser);
        testComment = TestDataFactory.createComment(1L, testUser, testActivity);

        commentRequest = new CommentRequest();
        commentRequest.setContent("Great activity!");

        BaseUserResponse userResponse = new BaseUserResponse();
        userResponse.setId(1L);
        userResponse.setFirstName("John");
        userResponse.setLastName("Doe");

        commentResponse = new CommentResponse();
        commentResponse.setId(1L);
        commentResponse.setContent("Great activity!");
        commentResponse.setCreatedAt(LocalDateTime.now());
        commentResponse.setUser(userResponse);
    }

    @Nested
    @DisplayName("GET /api/v1/activities/{activityId}/comments")
    class GetActivityCommentsTests {

        @Test
        @DisplayName("Should return activity comments")
        void shouldReturnActivityComments() throws Exception {
            given(commentService.getCommentsByActivityId(1L)).willReturn(List.of(testComment));
            given(commonMapper.convertToResponseList(anyList(), eq(CommentResponse.class)))
                    .willReturn(List.of(commentResponse));

            mockMvc.perform(get("/api/v1/activities/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].content").value("Great activity!"));
        }

        @Test
        @DisplayName("Should return empty list when no comments")
        void shouldReturnEmptyListWhenNoComments() throws Exception {
            given(commentService.getCommentsByActivityId(1L)).willReturn(List.of());
            given(commonMapper.convertToResponseList(anyList(), eq(CommentResponse.class)))
                    .willReturn(List.of());

            mockMvc.perform(get("/api/v1/activities/1/comments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/activities/{activityId}/comments")
    class CreateCommentTests {

        @Test
        @DisplayName("Should create comment successfully")
        void shouldCreateCommentSuccessfully() throws Exception {
            given(commentService.createComment(1L, 1L, "Great activity!")).willReturn(testComment);
            given(commonMapper.convertToResponse(testComment, CommentResponse.class)).willReturn(commentResponse);

            mockMvc.perform(post("/api/v1/activities/1/comments")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.content").value("Great activity!"));
        }

        @Test
        @DisplayName("Should return bad request when content is blank")
        void shouldReturnBadRequestWhenContentIsBlank() throws Exception {
            commentRequest.setContent("");

            mockMvc.perform(post("/api/v1/activities/1/comments")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserIdWhenNotProvided() throws Exception {
            given(commentService.createComment(1L, 1L, "Great activity!")).willReturn(testComment);
            given(commonMapper.convertToResponse(testComment, CommentResponse.class)).willReturn(commentResponse);

            mockMvc.perform(post("/api/v1/activities/1/comments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(commentRequest)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/activities/{activityId}/comments/{commentId}")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should update comment successfully")
        void shouldUpdateCommentSuccessfully() throws Exception {
            testComment.setContent("Updated comment");
            commentResponse.setContent("Updated comment");
            CommentRequest updateRequest = new CommentRequest();
            updateRequest.setContent("Updated comment");

            given(commentService.updateComment(1L, 1L, "Updated comment")).willReturn(testComment);
            given(commonMapper.convertToResponse(testComment, CommentResponse.class)).willReturn(commentResponse);

            mockMvc.perform(put("/api/v1/activities/1/comments/1")
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").value("Updated comment"));
        }

        @Test
        @DisplayName("Should return 403 when not comment author")
        void shouldReturn403WhenNotCommentAuthor() throws Exception {
            given(commentService.updateComment(1L, 2L, "Updated comment"))
                    .willThrow(new ApiRequestException("Not authorized", HttpStatus.FORBIDDEN));

            CommentRequest updateRequest = new CommentRequest();
            updateRequest.setContent("Updated comment");

            mockMvc.perform(put("/api/v1/activities/1/comments/1")
                            .param("userId", "2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/activities/{activityId}/comments/{commentId}")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should delete comment successfully")
        void shouldDeleteCommentSuccessfully() throws Exception {
            willDoNothing().given(commentService).deleteComment(1L, 1L);

            mockMvc.perform(delete("/api/v1/activities/1/comments/1")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Comment deleted"));
        }

        @Test
        @DisplayName("Should return 403 when not comment author")
        void shouldReturn403WhenNotCommentAuthor() throws Exception {
            willThrow(new ApiRequestException("Not authorized", HttpStatus.FORBIDDEN))
                    .given(commentService).deleteComment(1L, 2L);

            mockMvc.perform(delete("/api/v1/activities/1/comments/1")
                            .param("userId", "2"))
                    .andExpect(status().isForbidden());
        }
    }
}
