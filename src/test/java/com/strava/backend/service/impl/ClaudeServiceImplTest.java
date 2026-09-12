package com.strava.backend.service.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.strava.backend.TestGuardrails;
import com.strava.backend.configuration.ClaudeProperties;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService.ClaudeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("ClaudeServiceImpl")
class ClaudeServiceImplTest {

    private ClaudeProperties properties;
    private ClaudeServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new ClaudeProperties();
        service = new ClaudeServiceImpl(properties, TestGuardrails.recorder());
    }

    @Test
    @DisplayName("isConfigured reflects whether an api key is present")
    void isConfiguredReflectsApiKey() {
        assertThat(service.isConfigured()).isFalse();
        properties.setApiKey("sk-test");
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("chat throws 503 when not configured")
    void chatThrows503WhenNotConfigured() {
        assertThatThrownBy(() -> service.chat("system", List.of(new ClaudeMessage(ChatRole.USER, "hi"))))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("chat wraps SDK failures as 502 Bad Gateway")
    void chatWrapsSdkFailures() {
        properties.setApiKey("sk-test");

        AnthropicClient client = mock(AnthropicClient.class, RETURNS_DEEP_STUBS);
        when(client.messages().create(any(MessageCreateParams.class)))
                .thenThrow(new RuntimeException("network down"));
        service.setClient(client);

        assertThatThrownBy(() -> service.chat("sys", List.of(new ClaudeMessage(ChatRole.USER, "hi"))))
                .isInstanceOf(ApiRequestException.class)
                .extracting(e -> ((ApiRequestException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
