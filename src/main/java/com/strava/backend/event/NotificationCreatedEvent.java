package com.strava.backend.event;

import com.strava.backend.dto.NotificationDTO;
import lombok.Getter;

@Getter
public class NotificationCreatedEvent {

    private final Long recipientId;
    private final NotificationDTO notification;

    public NotificationCreatedEvent(Long recipientId, NotificationDTO notification) {
        this.recipientId = recipientId;
        this.notification = notification;
    }
}
