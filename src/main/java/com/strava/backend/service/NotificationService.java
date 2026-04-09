package com.strava.backend.service;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;
import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.enums.NotificationType;

import java.util.List;

public interface NotificationService {

    List<NotificationDTO> getNotifications(Long userId);

    long getUnreadCount(Long userId);

    void markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    void clearAllNotifications(Long userId);

    Notification createNotification(User recipient, User sender, NotificationType type,
                                    String message, Long referenceId, String referenceType);
}
