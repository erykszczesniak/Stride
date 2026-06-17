package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.dto.ai.ChatMessageResponse;
import com.strava.backend.dto.ai.ConversationResponse;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.service.AiChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AiChatController")
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    private ConversationResponse sampleConversation() {
        return new ConversationResponse(100L, List.of(
                new ChatMessageResponse(ChatRole.USER, "Analyze this activity.", LocalDateTime.now()),
                new ChatMessageResponse(ChatRole.ASSISTANT, "High-intensity group ride.", LocalDateTime.now())
        ));
    }

    @Nested
    @DisplayName("POST /api/v1/ai/activities/{id}/chat")
    class SendMessage {

        @Test
        @WithMockUser
        @DisplayName("returns 200 with the updated conversation")
        void returnsConversation() throws Exception {
            when(aiChatService.sendMessage(any(), eq(100L), anyString()))
                    .thenReturn(sampleConversation());

            mockMvc.perform(post("/api/v1/ai/activities/100/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "Analyze this activity."))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activityId").value(100))
                    .andExpect(jsonPath("$.messages").isArray())
                    .andExpect(jsonPath("$.messages[0].role").value("USER"))
                    .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"));
        }

        @Test
        @WithMockUser
        @DisplayName("returns 400 when the message is blank")
        void returns400WhenBlank() throws Exception {
            mockMvc.perform(post("/api/v1/ai/activities/100/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "   "))))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(aiChatService);
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void returns401() throws Exception {
            mockMvc.perform(post("/api/v1/ai/activities/100/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("message", "hi"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ai/activities/{id}/chat")
    class GetConversation {

        @Test
        @WithMockUser
        @DisplayName("returns 200 with the stored conversation")
        void returnsConversation() throws Exception {
            when(aiChatService.getConversation(any(), eq(100L))).thenReturn(sampleConversation());

            mockMvc.perform(get("/api/v1/ai/activities/100/chat"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messages.length()").value(2));
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void returns401() throws Exception {
            mockMvc.perform(get("/api/v1/ai/activities/100/chat"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/ai/activities/{id}/chat")
    class ClearConversation {

        @Test
        @WithMockUser
        @DisplayName("returns 204 and clears the conversation")
        void returns204() throws Exception {
            mockMvc.perform(delete("/api/v1/ai/activities/100/chat"))
                    .andExpect(status().isNoContent());

            verify(aiChatService).clearConversation(any(), eq(100L));
        }
    }
}
