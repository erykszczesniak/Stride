package com.strava.backend.event;

import com.strava.backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SseEmitterService sseEmitterService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        try {
            sseEmitterService.sendNotification(event.getRecipientId(), event.getNotification());
        } catch (Exception e) {
            log.warn("Failed to send SSE notification to user {}: {}", event.getRecipientId(), e.getMessage());
        }
    }
}
