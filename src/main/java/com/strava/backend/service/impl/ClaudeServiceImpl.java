package com.strava.backend.service.impl;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.strava.backend.ai.telemetry.AiCallContext;
import com.strava.backend.ai.telemetry.AiCallRecorder;
import com.strava.backend.configuration.ClaudeProperties;
import com.strava.backend.enums.ChatRole;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.service.ClaudeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Anthropic Claude implementation backed by the official anthropic-java SDK.
 *
 * <p>The SDK client is built lazily on first use (rather than as a bean) so the app boots even
 * when {@code ANTHROPIC_API_KEY} is unset — chat requests then fail fast with 503. The client is
 * settable so tests can inject a mock, mirroring how the project's other HTTP integrations swap
 * their {@code RestTemplate} in tests.
 *
 * <p>Every call is recorded by {@link AiCallRecorder}: latency, input/output tokens from the
 * response {@code usage}, the resulting cost and the outcome, attributed to the caller in
 * {@link AiCallContext}. {@link FailoverClaudeService} wraps this bean; inject the interface.
 */
@Service
@Slf4j
public class ClaudeServiceImpl implements ClaudeService {

    public static final String PROVIDER = "anthropic";

    private final ClaudeProperties properties;
    private final AiCallRecorder recorder;
    private volatile AnthropicClient client;

    public ClaudeServiceImpl(ClaudeProperties properties, AiCallRecorder recorder) {
        this.properties = properties;
        this.recorder = recorder;
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
        long started = System.nanoTime();
        AiCallContext.Ctx ctx = AiCallContext.current();
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

            Long inputTokens = null;
            Long outputTokens = null;
            try {
                if (response.usage() != null) {
                    inputTokens = response.usage().inputTokens();
                    outputTokens = response.usage().outputTokens();
                }
            } catch (RuntimeException ignored) {
                // usage is informational; a missing block must not fail the chat
            }

            if (text.isEmpty()) {
                record(ctx, started, inputTokens, outputTokens, AiCallRecorder.Outcome.EMPTY_RESPONSE, null);
                throw new ApiRequestException("Claude returned an empty response", HttpStatus.BAD_GATEWAY);
            }
            record(ctx, started, inputTokens, outputTokens, AiCallRecorder.Outcome.OK, null);
            return text;
        } catch (ApiRequestException e) {
            throw e;
        } catch (Exception e) {
            record(ctx, started, null, null, AiCallRecorder.Outcome.PROVIDER_ERROR, e.getClass().getSimpleName());
            log.error("Claude chat request failed: {}", e.getMessage(), e);
            throw new ApiRequestException("Failed to generate AI response", HttpStatus.BAD_GATEWAY);
        }
    }

    private void record(AiCallContext.Ctx ctx, long startedNanos, Long inputTokens, Long outputTokens,
                        AiCallRecorder.Outcome outcome, String error) {
        recorder.record(AiCallRecorder.Call.builder()
                .endpoint(AiCallRecorder.Endpoint.CHAT)
                .provider(PROVIDER)
                .model(properties.getModel())
                .userId(ctx == null ? null : ctx.userId())
                .activityId(ctx == null ? null : ctx.activityId())
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .duration(Duration.ofNanos(System.nanoTime() - startedNanos))
                .outcome(outcome)
                .error(error)
                .build());
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
