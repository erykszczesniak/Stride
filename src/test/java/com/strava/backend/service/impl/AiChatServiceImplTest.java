package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.AiConversation;
import com.strava.backend.domain.User;
import com.strava.backend.dto.ai.ConversationResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.AiConversationRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ClaudeService;
import com.strava.backend.service.ClaudeService.ClaudeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatServiceImpl")
class AiChatServiceImplTest {

    @Mock
    private AiConversationRepository conversationRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClaudeService claudeService;

    @InjectMocks
    private AiChatServiceImpl service;

    private User owner;
    private Activity activity;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("a@b.com").firstName("Ada").build();
        activity = Activity.builder()
                .id(100L)
                .title("Wednesday Group Ride")
                .activityType(ActivityType.RIDE)
                .distance(69.6)
                .duration(6900)
                .averagePower(213)
                .averageHeartRate(150)
                .elevationGain(540)
                .startTime(LocalDateTime.of(2026, 5, 13, 17, 15))
                .user(owner)
                .build();
    }

    @Test
    @DisplayName("sendMessage persists the user turn and Claude's reply, returning both")
    void sendMessagePersistsBothTurns() {
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(conversationRepository.findByUserIdAndActivityId(1L, 100L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(claudeService.chat(anyString(), any())).thenReturn("This was a high-intensity ride.");
        when(conversationRepository.save(any(AiConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConversationResponse response = service.sendMessage(1L, 100L, "Analyze this activity.");

        assertThat(response.activityId()).isEqualTo(100L);
        assertThat(response.messages()).hasSize(2);
        assertThat(response.messages().get(0).role()).isEqualTo(ChatRole.USER);
        assertThat(response.messages().get(0).content()).isEqualTo("Analyze this activity.");
        assertThat(response.messages().get(1).role()).isEqualTo(ChatRole.ASSISTANT);
        assertThat(response.messages().get(1).content()).isEqualTo("This was a high-intensity ride.");
        verify(conversationRepository).save(any(AiConversation.class));
    }

    @Test
    @DisplayName("sendMessage forwards prior history plus the new turn to Claude")
    void sendMessageForwardsHistory() {
        AiConversation existing = AiConversation.builder().user(owner).activity(activity).build();
        existing.addMessage(ChatRole.USER, "Hi");
        existing.addMessage(ChatRole.ASSISTANT, "Hello, how can I help?");

        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(conversationRepository.findByUserIdAndActivityId(1L, 100L)).thenReturn(Optional.of(existing));
        when(claudeService.chat(anyString(), any())).thenReturn("Sure.");
        when(conversationRepository.save(any(AiConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendMessage(1L, 100L, "How was my pacing?");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClaudeMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(claudeService).chat(anyString(), captor.capture());
        List<ClaudeMessage> sent = captor.getValue();
        assertThat(sent).hasSize(3);
        assertThat(sent.get(2).role()).isEqualTo(ChatRole.USER);
        assertThat(sent.get(2).content()).isEqualTo("How was my pacing?");
    }

    @Test
    @DisplayName("sendMessage rejects an activity owned by another user as not-found")
    void sendMessageRejectsForeignActivity() {
        User other = User.builder().id(2L).build();
        activity.setUser(other);
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.sendMessage(1L, 100L, "hi"))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(claudeService);
    }

    @Test
    @DisplayName("does not persist anything when Claude fails")
    void doesNotPersistWhenClaudeFails() {
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(conversationRepository.findByUserIdAndActivityId(1L, 100L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(claudeService.chat(anyString(), any()))
                .thenThrow(new ApiRequestException("boom", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.sendMessage(1L, 100L, "Analyze"))
                .isInstanceOf(ApiRequestException.class);

        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("getConversation returns an empty conversation when none exists")
    void getConversationEmpty() {
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(conversationRepository.findByUserIdAndActivityId(1L, 100L)).thenReturn(Optional.empty());

        ConversationResponse response = service.getConversation(1L, 100L);

        assertThat(response.activityId()).isEqualTo(100L);
        assertThat(response.messages()).isEmpty();
    }

    @Test
    @DisplayName("getConversation throws not-found for a missing activity")
    void getConversationMissingActivity() {
        when(activityRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConversation(1L, 999L))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("clearConversation deletes an existing conversation")
    void clearConversationDeletes() {
        AiConversation existing = AiConversation.builder().user(owner).activity(activity).build();
        when(activityRepository.findById(100L)).thenReturn(Optional.of(activity));
        when(conversationRepository.findByUserIdAndActivityId(1L, 100L)).thenReturn(Optional.of(existing));

        service.clearConversation(1L, 100L);

        verify(conversationRepository).delete(existing);
    }

    @Test
    @DisplayName("buildSystemPrompt grounds Claude in the activity's concrete metrics")
    void buildSystemPromptIncludesMetrics() {
        String prompt = service.buildSystemPrompt(activity);

        assertThat(prompt)
                .contains("endurance coach")
                .contains("RIDE")
                .contains("Wednesday Group Ride")
                .contains("69.6")     // distance
                .contains("213")      // average power
                .contains("540")      // elevation gain
                .contains("Average pace");
        // Metrics that are null must not leak placeholder lines.
        assertThat(prompt).doesNotContain("Calories");
    }
}
