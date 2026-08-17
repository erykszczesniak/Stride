package com.strava.aiservice.chat

import com.strava.aiservice.client.ActivityContext
import com.strava.aiservice.client.ActivityContextClient
import com.strava.aiservice.client.ChatMessage
import com.strava.aiservice.client.ClaudeChatClient
import com.strava.aiservice.domain.AiConversation
import com.strava.aiservice.domain.AiConversationRepository
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.Optional

/** Port of the monolith's AiChatServiceImplTest against the microservice collaborators. */
@DisplayName("AiChatService")
class AiChatServiceTest {

    private val conversationRepository: AiConversationRepository = mock()
    private val activityClient: ActivityContextClient = mock()
    private val claudeClient: ClaudeChatClient = mock()

    private val service = AiChatService(conversationRepository, activityClient, claudeClient)

    private lateinit var activity: ActivityContext

    @BeforeEach
    fun setUp() {
        activity = ActivityContext(
            id = 100,
            userId = 1,
            title = "Wednesday Group Ride",
            activityType = "RIDE",
            distance = 69.6,
            duration = 6900,
            averagePower = 213,
            averageHeartRate = 150,
            elevationGain = 540,
            startTime = LocalDateTime.of(2026, 5, 13, 17, 15)
        )
    }

    @Test
    @DisplayName("sendMessage persists the user turn and Claude's reply, returning both")
    fun sendMessagePersistsBothTurns() {
        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.empty())
        whenever(claudeClient.chat(any(), any())).thenReturn("This was a high-intensity ride.")
        whenever(conversationRepository.save(any<AiConversation>())).doAnswer { it.getArgument(0) }

        val response = service.sendMessage(1, 100, "Analyze this activity.")

        assertThat(response.activityId).isEqualTo(100)
        assertThat(response.messages).hasSize(2)
        assertThat(response.messages[0].role).isEqualTo(ChatRole.USER)
        assertThat(response.messages[0].content).isEqualTo("Analyze this activity.")
        assertThat(response.messages[1].role).isEqualTo(ChatRole.ASSISTANT)
        assertThat(response.messages[1].content).isEqualTo("This was a high-intensity ride.")
        verify(conversationRepository).save(any<AiConversation>())
    }

    @Test
    @DisplayName("sendMessage forwards prior history plus the new turn to Claude")
    fun sendMessageForwardsHistory() {
        val existing = AiConversation(userId = 1, activityId = 100)
        existing.addMessage(ChatRole.USER, "Hi")
        existing.addMessage(ChatRole.ASSISTANT, "Hello, how can I help?")

        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.of(existing))
        whenever(claudeClient.chat(any(), any())).thenReturn("Sure.")
        whenever(conversationRepository.save(any<AiConversation>())).doAnswer { it.getArgument(0) }

        service.sendMessage(1, 100, "How was my pacing?")

        val captor = argumentCaptor<List<ChatMessage>>()
        verify(claudeClient).chat(any(), captor.capture())
        val sent = captor.firstValue
        assertThat(sent).hasSize(3)
        assertThat(sent[2].role).isEqualTo(ChatRole.USER)
        assertThat(sent[2].content).isEqualTo("How was my pacing?")
    }

    @Test
    @DisplayName("sendMessage rejects an activity owned by another user as not-found")
    fun sendMessageRejectsForeignActivity() {
        whenever(activityClient.findActivity(100)).thenReturn(activity.copy(userId = 2))

        assertThatThrownBy { service.sendMessage(1, 100, "hi") }
            .isInstanceOf(ApiRequestException::class.java)
            .hasMessage("Activity not found")
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.NOT_FOUND)

        verifyNoInteractions(claudeClient)
    }

    @Test
    @DisplayName("does not persist anything when Claude fails")
    fun doesNotPersistWhenClaudeFails() {
        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.empty())
        whenever(claudeClient.chat(any(), any()))
            .thenThrow(ApiRequestException("boom", HttpStatus.BAD_GATEWAY))

        assertThatThrownBy { service.sendMessage(1, 100, "Analyze") }
            .isInstanceOf(ApiRequestException::class.java)

        verify(conversationRepository, never()).save(anyOrNull())
    }

    @Test
    @DisplayName("getConversation returns an empty conversation when none exists")
    fun getConversationEmpty() {
        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.empty())

        val response = service.getConversation(1, 100)

        assertThat(response.activityId).isEqualTo(100)
        assertThat(response.messages).isEmpty()
    }

    @Test
    @DisplayName("getConversation throws not-found for a missing activity")
    fun getConversationMissingActivity() {
        whenever(activityClient.findActivity(999)).thenReturn(null)

        assertThatThrownBy { service.getConversation(1, 999) }
            .isInstanceOf(ApiRequestException::class.java)
            .extracting { (it as ApiRequestException).status }
            .isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    @DisplayName("clearConversation deletes an existing conversation")
    fun clearConversationDeletes() {
        val existing = AiConversation(userId = 1, activityId = 100)
        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.of(existing))

        service.clearConversation(1, 100)

        verify(conversationRepository).delete(existing)
    }

    @Test
    @DisplayName("clearConversation still checks ownership")
    fun clearConversationChecksOwnership() {
        whenever(activityClient.findActivity(100)).thenReturn(activity.copy(userId = 2))

        assertThatThrownBy { service.clearConversation(1, 100) }
            .isInstanceOf(ApiRequestException::class.java)

        verify(conversationRepository, never()).delete(anyOrNull())
    }

    @Test
    @DisplayName("buildSystemPrompt grounds Claude in the activity's concrete metrics")
    fun buildSystemPromptIncludesMetrics() {
        val prompt = service.buildSystemPrompt(activity)

        assertThat(prompt)
            .contains("endurance coach")
            .contains("RIDE")
            .contains("Wednesday Group Ride")
            .contains("69.6") // distance
            .contains("213") // average power
            .contains("540") // elevation gain
            .contains("Average pace")
        // Metrics that are null must not leak placeholder lines.
        assertThat(prompt).doesNotContain("Calories")
    }

    @Test
    @DisplayName("buildSystemPrompt appends the training history block with the discussed marker")
    fun buildSystemPromptIncludesHistory() {
        val history = listOf(
            activity,
            ActivityContext(id = 99, title = "Easy Jog", activityType = "RUN", distance = 5.5,
                duration = 1980, averageHeartRate = 138, elevationGain = 20,
                startTime = LocalDateTime.of(2026, 5, 11, 7, 0))
        )

        val prompt = service.buildSystemPrompt(activity, history)

        assertThat(prompt)
            .contains("Recent training history")
            .contains("2026-05-11 | RUN | \"Easy Jog\" | 5.5 km")
            .contains("avg HR 138")
            .contains("<- discussed")
        // The discussed marker must sit on the current activity's line only.
        val discussedLine = prompt.lines().single { it.endsWith("<- discussed") }
        assertThat(discussedLine).contains("Wednesday Group Ride")
    }

    @Test
    @DisplayName("buildSystemPrompt omits the history block when the history is empty")
    fun buildSystemPromptWithoutHistory() {
        assertThat(service.buildSystemPrompt(activity, emptyList()))
            .doesNotContain("Recent training history")
    }

    @Test
    @DisplayName("sendMessage feeds the recent history into the system prompt")
    fun sendMessageUsesHistoryContext() {
        whenever(activityClient.findActivity(100)).thenReturn(activity)
        whenever(activityClient.recentActivities(1, AiChatService.HISTORY_LIMIT))
            .thenReturn(listOf(activity))
        whenever(conversationRepository.findByUserIdAndActivityId(1, 100)).thenReturn(Optional.empty())
        whenever(conversationRepository.save(anyOrNull())).thenAnswer { it.arguments[0] }
        val systemPrompt = argumentCaptor<String>()
        whenever(claudeClient.chat(systemPrompt.capture(), anyOrNull())).thenReturn("ok")

        service.sendMessage(1, 100, "how was my week?")

        assertThat(systemPrompt.firstValue).contains("Recent training history")
        verify(activityClient).recentActivities(1, AiChatService.HISTORY_LIMIT)
    }
}
