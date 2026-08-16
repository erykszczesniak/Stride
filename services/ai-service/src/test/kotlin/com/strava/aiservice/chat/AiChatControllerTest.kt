package com.strava.aiservice.chat

import com.strava.aiservice.config.SecurityConfig
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

/**
 * Contract slice test: paths, auth (gateway-injected X-User-* headers) and
 * response shapes must match the monolith's AiChatControllerTest.
 */
@WebMvcTest(
    controllers = [AiChatController::class],
    properties = [
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
    ]
)
@Import(SecurityConfig::class)
@DisplayName("AiChatController")
class AiChatControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var aiChatService: AiChatService

    private fun sampleConversation() = ConversationResponse(
        100,
        listOf(
            ChatMessageResponse(ChatRole.USER, "Analyze this activity.", LocalDateTime.of(2026, 5, 13, 18, 0)),
            ChatMessageResponse(ChatRole.ASSISTANT, "High-intensity group ride.", LocalDateTime.of(2026, 5, 13, 18, 0, 5))
        )
    )

    @Test
    @DisplayName("POST returns 200 with the updated conversation")
    fun postReturnsConversation() {
        whenever(aiChatService.sendMessage(eq(1L), eq(100L), any())).thenReturn(sampleConversation())

        mockMvc.perform(
            post("/api/v1/ai/activities/100/chat")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message": "Analyze this activity."}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityId").value(100))
            .andExpect(jsonPath("$.messages").isArray)
            .andExpect(jsonPath("$.messages[0].role").value("USER"))
            .andExpect(jsonPath("$.messages[0].content").value("Analyze this activity."))
            .andExpect(jsonPath("$.messages[0].createdAt").value("2026-05-13T18:00:00"))
            .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
            .andExpect(jsonPath("$.messages[1].content").value("High-intensity group ride."))

        verify(aiChatService).sendMessage(1L, 100L, "Analyze this activity.")
    }

    @Test
    @DisplayName("POST returns 400 when the message is blank")
    fun postReturns400WhenBlank() {
        mockMvc.perform(
            post("/api/v1/ai/activities/100/chat")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message": "   "}""")
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(aiChatService)
    }

    @Test
    @DisplayName("POST returns 400 when the message is missing")
    fun postReturns400WhenMissing() {
        mockMvc.perform(
            post("/api/v1/ai/activities/100/chat")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)

        verifyNoInteractions(aiChatService)
    }

    @Test
    @DisplayName("POST surfaces the service's not-found as 404 with a message property")
    fun postSurfacesNotFound() {
        whenever(aiChatService.sendMessage(eq(1L), eq(999L), any()))
            .thenThrow(ApiRequestException("Activity not found", HttpStatus.NOT_FOUND))

        mockMvc.perform(
            post("/api/v1/ai/activities/999/chat")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message": "hi"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Activity not found"))
    }

    @Test
    @DisplayName("requests without gateway identity headers are rejected")
    fun postWithoutIdentityIsRejected() {
        mockMvc.perform(
            post("/api/v1/ai/activities/100/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"message": "hi"}""")
        )
            .andExpect(status().isForbidden)

        verifyNoInteractions(aiChatService)
    }

    @Test
    @DisplayName("GET returns 200 with the stored conversation")
    fun getReturnsConversation() {
        whenever(aiChatService.getConversation(1L, 100L)).thenReturn(sampleConversation())

        mockMvc.perform(get("/api/v1/ai/activities/100/chat").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.messages.length()").value(2))
    }

    @Test
    @DisplayName("GET returns the empty-conversation shape when none exists")
    fun getReturnsEmptyShape() {
        whenever(aiChatService.getConversation(1L, 100L)).thenReturn(ConversationResponse(100, emptyList()))

        mockMvc.perform(get("/api/v1/ai/activities/100/chat").header("X-User-Id", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.activityId").value(100))
            .andExpect(jsonPath("$.messages").isArray)
            .andExpect(jsonPath("$.messages.length()").value(0))
    }

    @Test
    @DisplayName("GET without identity headers is rejected")
    fun getWithoutIdentityIsRejected() {
        mockMvc.perform(get("/api/v1/ai/activities/100/chat"))
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("DELETE returns 204 and clears the conversation")
    fun deleteReturns204() {
        mockMvc.perform(delete("/api/v1/ai/activities/100/chat").header("X-User-Id", "1"))
            .andExpect(status().isNoContent)

        verify(aiChatService).clearConversation(1L, 100L)
    }
}
