package com.strava.backend.service.impl;

import com.strava.backend.TestGuardrails;
import com.strava.backend.ai.guard.AiGuardProperties;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService.ClaudeMessage;
import com.strava.backend.service.GroqService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FailoverClaudeService")
class FailoverClaudeServiceTest {

    private final ClaudeServiceImpl primary = mock(ClaudeServiceImpl.class);
    private final GroqService secondary = mock(GroqService.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AiGuardProperties properties = TestGuardrails.properties();
    private final FailoverClaudeService service =
            new FailoverClaudeService(primary, secondary, properties, TestGuardrails.recorder(properties, registry));
    private final List<ClaudeMessage> turn = List.of(new ClaudeMessage(ChatRole.USER, "how was my run?"));

    @BeforeEach
    void setUp() {
        when(primary.isConfigured()).thenReturn(true);
        when(secondary.isConfigured()).thenReturn(true);
    }

    private double failovers() {
        Counter c = registry.find("ai.failover").tag("to", "groq").counter();
        return c == null ? 0.0 : c.count();
    }

    @Test
    @DisplayName("a healthy primary answers and nothing fails over")
    void primaryAnswers() {
        when(primary.chat(anyString(), any())).thenReturn("Nice tempo.");

        assertThat(service.chat("sys", turn)).isEqualTo("Nice tempo.");
        verify(secondary, never()).chatCompletion(anyString(), any());
        assertThat(failovers()).isZero();
    }

    @Test
    @DisplayName("a 502 from the primary routes the same conversation to Groq and counts the failover")
    void providerErrorFailsOver() {
        when(primary.chat(anyString(), any())).thenThrow(new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY));
        when(secondary.chatCompletion(eq("sys"), eq(turn))).thenReturn("Groq says: solid.");

        assertThat(service.chat("sys", turn)).isEqualTo("Groq says: solid.");
        assertThat(failovers()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("an unconfigured primary goes straight to Groq")
    void unconfiguredPrimaryUsesSecondary() {
        when(primary.isConfigured()).thenReturn(false);
        when(secondary.chatCompletion(anyString(), any())).thenReturn("groq");

        assertThat(service.isConfigured()).isTrue();
        assertThat(service.chat("sys", turn)).isEqualTo("groq");
        verify(primary, never()).chat(anyString(), any());
        assertThat(failovers()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("caller errors are not retried on the secondary")
    void callerErrorsAreRethrown() {
        when(primary.chat(anyString(), any())).thenThrow(new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.chat("sys", turn))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(secondary, never()).chatCompletion(anyString(), any());
        assertThat(failovers()).isZero();
    }

    @Test
    @DisplayName("with failover disabled the primary's error surfaces unchanged")
    void disabledFailoverRethrows() {
        properties.getFailover().setEnabled(false);
        when(primary.chat(anyString(), any())).thenThrow(new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.chat("sys", turn))
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        verify(secondary, never()).chatCompletion(anyString(), any());
    }

    @Test
    @DisplayName("neither provider configured is a 503, not a failover")
    void nothingConfiguredIs503() {
        when(primary.isConfigured()).thenReturn(false);
        when(secondary.isConfigured()).thenReturn(false);

        assertThat(service.isConfigured()).isFalse();
        assertThatThrownBy(() -> service.chat("sys", turn))
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(failovers()).isZero();
        verify(secondary, never()).chatCompletion(anyString(), any());
    }

    @Test
    @DisplayName("when the secondary fails as well the caller gets one 502")
    void bothFailIs502() {
        when(primary.chat(anyString(), any())).thenThrow(new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY));
        when(secondary.chatCompletion(anyString(), any())).thenThrow(new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> service.chat("sys", turn))
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(failovers()).isEqualTo(1.0);
    }
}
