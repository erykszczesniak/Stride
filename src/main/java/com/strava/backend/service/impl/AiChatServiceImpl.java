package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.AiConversation;
import com.strava.backend.domain.User;
import com.strava.backend.dto.ai.ChatMessageResponse;
import com.strava.backend.dto.ai.ConversationResponse;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.AiConversationRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.AiChatService;
import com.strava.backend.service.ClaudeService;
import com.strava.backend.service.ClaudeService.ClaudeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.strava.backend.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static com.strava.backend.constants.ErrorMessage.USER_NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConversationRepository conversationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ClaudeService claudeService;

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long userId, Long activityId) {
        // Validate ownership so callers can't probe other users' activities by id.
        requireOwnedActivity(userId, activityId);
        return conversationRepository.findByUserIdAndActivityId(userId, activityId)
                .map(this::toResponse)
                .orElseGet(() -> new ConversationResponse(activityId, List.of()));
    }

    @Override
    @Transactional
    public ConversationResponse sendMessage(Long userId, Long activityId, String message) {
        Activity activity = requireOwnedActivity(userId, activityId);

        AiConversation conversation = conversationRepository.findByUserIdAndActivityId(userId, activityId)
                .orElseGet(() -> newConversation(userId, activity));

        // Build the request from the stored history plus the new user turn.
        List<ClaudeMessage> request = new ArrayList<>();
        conversation.getMessages()
                .forEach(m -> request.add(new ClaudeMessage(m.getRole(), m.getContent())));
        request.add(new ClaudeMessage(ChatRole.USER, message));

        // If Claude fails the transaction rolls back, so neither turn is persisted.
        String reply = claudeService.chat(buildSystemPrompt(activity), request);

        conversation.addMessage(ChatRole.USER, message);
        conversation.addMessage(ChatRole.ASSISTANT, reply);
        AiConversation saved = conversationRepository.save(conversation);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void clearConversation(Long userId, Long activityId) {
        requireOwnedActivity(userId, activityId);
        conversationRepository.findByUserIdAndActivityId(userId, activityId)
                .ifPresent(conversationRepository::delete);
    }

    private AiConversation newConversation(Long userId, Activity activity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
        return AiConversation.builder().user(user).activity(activity).build();
    }

    /**
     * Loads the activity and confirms it belongs to {@code userId}. A foreign activity is reported
     * as not-found rather than forbidden so we don't leak which activity ids exist.
     */
    private Activity requireOwnedActivity(Long userId, Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (activity.getUser() == null || !activity.getUser().getId().equals(userId)) {
            throw new ApiRequestException(ACTIVITY_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        return activity;
    }

    private ConversationResponse toResponse(AiConversation conversation) {
        List<ChatMessageResponse> messages = conversation.getMessages().stream()
                .map(m -> new ChatMessageResponse(m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new ConversationResponse(conversation.getActivity().getId(), messages);
    }

    /**
     * Grounds Claude in this specific activity: a coaching persona plus the available metrics.
     * Only non-null metrics are listed so the model never reasons about absent data, and it's told
     * to stick to what's provided rather than inventing numbers.
     */
    String buildSystemPrompt(Activity activity) {
        StringBuilder facts = new StringBuilder();
        facts.append("- Type: ").append(activity.getActivityType()).append('\n');
        facts.append("- Title: ").append(activity.getTitle()).append('\n');
        if (activity.getStartTime() != null) {
            facts.append("- Date: ").append(activity.getStartTime()).append('\n');
        }
        appendIfPresent(facts, "Distance (km)", activity.getDistance());
        if (activity.getDuration() != null) {
            facts.append("- Duration: ").append(formatDuration(activity.getDuration())).append('\n');
        }
        appendPace(facts, activity);
        appendIfPresent(facts, "Average speed (km/h)", activity.getAverageSpeed());
        appendIfPresent(facts, "Max speed (km/h)", activity.getMaxSpeed());
        appendIfPresent(facts, "Elevation gain (m)", activity.getElevationGain());
        appendIfPresent(facts, "Average heart rate (bpm)", activity.getAverageHeartRate());
        appendIfPresent(facts, "Max heart rate (bpm)", activity.getMaxHeartRate());
        appendIfPresent(facts, "Average power (W)", activity.getAveragePower());
        appendIfPresent(facts, "Average cadence", activity.getAverageCadence());
        appendIfPresent(facts, "Calories", activity.getCalories());
        if (activity.getDescription() != null && !activity.getDescription().isBlank()) {
            facts.append("- Athlete's note: ").append(activity.getDescription()).append('\n');
        }

        return """
                You are an expert endurance coach analysing a single training activity for an athlete \
                on a fitness-tracking app. Be concise, encouraging, and specific. Reference concrete \
                numbers from the activity data and explain what they mean for training. Only use the \
                metrics provided below — never invent data that isn't present. If the athlete asks \
                something the data can't answer, say so briefly.

                Activity data:
                """ + facts;
    }

    private void appendPace(StringBuilder sb, Activity activity) {
        Double distance = activity.getDistance();
        Integer duration = activity.getDuration();
        if (distance != null && distance > 0 && duration != null && duration > 0) {
            double secondsPerKm = duration / distance;
            int minutes = (int) (secondsPerKm / 60);
            int seconds = (int) Math.round(secondsPerKm % 60);
            if (seconds == 60) {
                minutes++;
                seconds = 0;
            }
            sb.append("- Average pace: ")
                    .append(String.format(Locale.ROOT, "%d:%02d /km", minutes, seconds))
                    .append('\n');
        }
    }

    private void appendIfPresent(StringBuilder sb, String label, Number value) {
        if (value != null) {
            sb.append("- ").append(label).append(": ").append(value).append('\n');
        }
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        if (hours > 0) {
            return String.format(Locale.ROOT, "%dh %02dm %02ds", hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%dm %02ds", minutes, secs);
    }
}
