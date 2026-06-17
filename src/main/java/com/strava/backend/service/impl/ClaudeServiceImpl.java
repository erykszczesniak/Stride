package com.strava.backend.service.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.strava.backend.configuration.ClaudeProperties;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Anthropic Claude implementation backed by the official anthropic-java SDK.
 *
 * <p>The SDK client is built lazily on first use (rather than as a bean) so the app boots even
 * when {@code ANTHROPIC_API_KEY} is unset — chat requests then fail fast with 503. The client is
 * settable so tests can inject a mock, mirroring how the project's other HTTP integrations swap
 * their {@code RestTemplate} in tests.
 */
@Service
@Slf4j
public class ClaudeServiceImpl implements ClaudeService {

    private final ClaudeProperties properties;
    private volatile AnthropicClient client;

    public ClaudeServiceImpl(ClaudeProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String chat(String systemPrompt, List<ClaudeMessage> conversation) {
        if (!isConfigured()) {
            throw new ApiRequestException("AI chat is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        try {
            MessageCreateParams.Builder builder = MessageCreateParams.builder()
                    .model(properties.getModel())
                    .maxTokens(properties.getMaxTokens())
                    .system(systemPrompt);

            for (ClaudeMessage message : conversation) {
                if (message.role() == ChatRole.ASSISTANT) {
                    builder.addAssistantMessage(message.content());
                } else {
                    builder.addUserMessage(message.content());
                }
            }

            Message response = anthropicClient().messages().create(builder.build());

            String text = response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(textBlock -> textBlock.text())
                    .collect(Collectors.joining())
                    .trim();

            if (text.isEmpty()) {
                throw new ApiRequestException("Claude returned an empty response", HttpStatus.BAD_GATEWAY);
            }
            return text;
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Claude chat request failed: {}", e.getMessage(), e);
            throw new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY);
        }
    }

    private AnthropicClient anthropicClient() {
        AnthropicClient local = this.client;
        if (local == null) {
            synchronized (this) {
                local = this.client;
                if (local == null) {
                    local = AnthropicOkHttpClient.builder()
                            .apiKey(properties.getApiKey())
                            .build();
                    this.client = local;
                }
            }
        }
        return local;
    }

    /** Test seam: inject a mock {@link AnthropicClient} instead of hitting the real API. */
    void setClient(AnthropicClient client) {
        this.client = client;
    }
}
