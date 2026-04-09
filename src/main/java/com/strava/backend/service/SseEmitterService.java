package com.strava.backend.service;

import com.strava.backend.dto.NotificationDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {

    SseEmitter createEmitter(Long userId);

    void sendNotification(Long userId, NotificationDTO notification);

    void removeEmitter(Long userId);

    boolean hasActiveEmitter(Long userId);
}
