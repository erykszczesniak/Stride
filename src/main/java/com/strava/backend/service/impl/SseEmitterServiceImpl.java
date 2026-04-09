package com.strava.backend.service.impl;

import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.service.SseEmitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterServiceImpl implements SseEmitterService {

    private static final long SSE_TIMEOUT = 300_000L; // 5 minutes

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter createEmitter(Long userId) {
        removeEmitter(userId);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE connection completed for user {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE connection timed out for user {}", userId);
            emitters.remove(userId);
        });

        emitter.onError(e -> {
            log.debug("SSE connection error for user {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        });

        emitters.put(userId, emitter);
        log.debug("SSE emitter created for user {}", userId);

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connection established"));
        } catch (IOException e) {
            log.debug("Failed to send initial SSE event to user {}", userId);
            emitters.remove(userId);
        }

        return emitter;
    }

    @Override
    public void sendNotification(Long userId, NotificationDTO notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            log.debug("SSE notification sent to user {}", userId);
        } catch (IOException e) {
            log.debug("Failed to send SSE notification to user {}, removing emitter", userId);
            emitters.remove(userId);
        }
    }

    @Scheduled(fixedRate = 25000)
    public void sendHeartbeats() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.debug("Heartbeat failed for user {}, removing emitter", userId);
                emitters.remove(userId);
            }
        });
    }

    @Override
    public void removeEmitter(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // Emitter already completed or errored
            }
        }
    }

    @Override
    public boolean hasActiveEmitter(Long userId) {
        return emitters.containsKey(userId);
    }
}
