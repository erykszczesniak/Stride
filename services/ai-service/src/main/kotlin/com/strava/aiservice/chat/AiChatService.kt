package com.strava.aiservice.chat

import com.strava.aiservice.client.ActivityContext
import com.strava.aiservice.client.ActivityContextClient
import com.strava.aiservice.client.ChatMessage
import com.strava.aiservice.client.ClaudeChatClient
import com.strava.aiservice.domain.AiConversation
import com.strava.aiservice.domain.AiConversationRepository
import com.strava.aiservice.domain.ChatRole
import com.strava.aiservice.exception.ApiRequestException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Port of the monolith's AiChatServiceImpl. Differences forced by
 * database-per-service: the activity is fetched from activity-service's
 * internal API instead of a local repository, and the conversation stores the
 * authenticated userId directly (no User lookup — identity comes verified from
 * the gateway, so main's USER_NOT_FOUND branch has no equivalent here).
 */
@Service
class AiChatService(
    private val conversationRepository: AiConversationRepository,
    private val activityClient: ActivityContextClient,
    private val claudeClient: ClaudeChatClient
) {

    @Transactional(readOnly = true)
    fun getConversation(userId: Long, activityId: Long): ConversationResponse {
        // Validate ownership so callers can't probe other users' activities by id.
        requireOwnedActivity(userId, activityId)
        return conversationRepository.findByUserIdAndActivityId(userId, activityId)
            .map { toResponse(it) }
            .orElseGet { ConversationResponse(activityId, emptyList()) }
    }

    @Transactional
    fun sendMessage(userId: Long, activityId: Long, message: String): ConversationResponse {
        val activity = requireOwnedActivity(userId, activityId)

        val conversation = conversationRepository.findByUserIdAndActivityId(userId, activityId)
            .orElseGet { AiConversation(userId = userId, activityId = activityId) }

        // Build the request from the stored history plus the new user turn.
        val request = mutableListOf<ChatMessage>()
        conversation.messages.forEach { request.add(ChatMessage(it.role, it.content)) }
        request.add(ChatMessage(ChatRole.USER, message))

        // If Claude fails the transaction rolls back, so neither turn is persisted.
        val reply = claudeClient.chat(buildSystemPrompt(activity), request)

        conversation.addMessage(ChatRole.USER, message)
        conversation.addMessage(ChatRole.ASSISTANT, reply)
        val saved = conversationRepository.save(conversation)

        return toResponse(saved)
    }

    @Transactional
    fun clearConversation(userId: Long, activityId: Long) {
        requireOwnedActivity(userId, activityId)
        conversationRepository.findByUserIdAndActivityId(userId, activityId)
            .ifPresent { conversationRepository.delete(it) }
    }

    /**
     * Loads the activity and confirms it belongs to [userId]. A foreign activity is reported
     * as not-found rather than forbidden so we don't leak which activity ids exist —
     * same rule and status as main's requireOwnedActivity.
     */
    private fun requireOwnedActivity(userId: Long, activityId: Long): ActivityContext {
        val activity = activityClient.findActivity(activityId)
            ?: throw ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND)
        if (activity.userId == null || activity.userId != userId) {
            throw ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND)
        }
        return activity
    }

    private fun toResponse(conversation: AiConversation): ConversationResponse {
        val messages = conversation.messages.map { ChatMessageResponse(it.role, it.content, it.createdAt) }
        return ConversationResponse(conversation.activityId, messages)
    }

    /**
     * Grounds Claude in this specific activity: a coaching persona plus the available metrics.
     * Only non-null metrics are listed so the model never reasons about absent data, and it's told
     * to stick to what's provided rather than inventing numbers. Ported verbatim from main's
     * AiChatServiceImpl.buildSystemPrompt.
     */
    internal fun buildSystemPrompt(activity: ActivityContext): String {
        val facts = StringBuilder()
        facts.append("- Type: ").append(activity.activityType).append('\n')
        facts.append("- Title: ").append(activity.title).append('\n')
        if (activity.startTime != null) {
            facts.append("- Date: ").append(activity.startTime).append('\n')
        }
        appendIfPresent(facts, "Distance (km)", activity.distance)
        if (activity.duration != null) {
            facts.append("- Duration: ").append(formatDuration(activity.duration)).append('\n')
        }
        appendPace(facts, activity)
        appendIfPresent(facts, "Average speed (km/h)", activity.averageSpeed)
        appendIfPresent(facts, "Max speed (km/h)", activity.maxSpeed)
        appendIfPresent(facts, "Elevation gain (m)", activity.elevationGain)
        appendIfPresent(facts, "Average heart rate (bpm)", activity.averageHeartRate)
        appendIfPresent(facts, "Max heart rate (bpm)", activity.maxHeartRate)
        appendIfPresent(facts, "Average power (W)", activity.averagePower)
        appendIfPresent(facts, "Average cadence", activity.averageCadence)
        appendIfPresent(facts, "Calories", activity.calories)
        if (!activity.description.isNullOrBlank()) {
            facts.append("- Athlete's note: ").append(activity.description).append('\n')
        }

        return "You are an expert endurance coach analysing a single training activity for an athlete " +
            "on a fitness-tracking app. Be concise, encouraging, and specific. Reference concrete " +
            "numbers from the activity data and explain what they mean for training. Only use the " +
            "metrics provided below — never invent data that isn't present. If the athlete asks " +
            "something the data can't answer, say so briefly.\n" +
            "\n" +
            "Activity data:\n" + facts
    }

    private fun appendPace(sb: StringBuilder, activity: ActivityContext) {
        val distance = activity.distance
        val duration = activity.duration
        if (distance != null && distance > 0 && duration != null && duration > 0) {
            val secondsPerKm = duration / distance
            var minutes = (secondsPerKm / 60).toInt()
            var seconds = (secondsPerKm % 60).roundToInt()
            if (seconds == 60) {
                minutes++
                seconds = 0
            }
            sb.append("- Average pace: ")
                .append(String.format(Locale.ROOT, "%d:%02d /km", minutes, seconds))
                .append('\n')
        }
    }

    private fun appendIfPresent(sb: StringBuilder, label: String, value: Number?) {
        if (value != null) {
            sb.append("- ").append(label).append(": ").append(value).append('\n')
        }
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.ROOT, "%dh %02dm %02ds", hours, minutes, secs)
        } else {
            String.format(Locale.ROOT, "%dm %02ds", minutes, secs)
        }
    }

    companion object {
        const val ACTIVITY_NOT_FOUND = "Activity not found"
    }
}
