package com.strava.backend.service.impl;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;
import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.event.NotificationCreatedEvent;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.NotificationRepository;
import com.strava.backend.service.EmailNotificationService;
import com.strava.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiRequestException("Notification not found", HttpStatus.NOT_FOUND));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ApiRequestException("Access denied", HttpStatus.FORBIDDEN);
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }

    @Override
    @Transactional
    public Notification createNotification(User recipient, User sender, NotificationType type,
                                           String message, Long referenceId, String referenceType) {
        if (sender != null && recipient.getId().equals(sender.getId())) {
            return null;
        }

        if (type == NotificationType.LIKE && sender != null && referenceId != null &&
                notificationRepository.existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
                        recipient.getId(), sender.getId(), type, referenceId)) {
            return null;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        try {
            emailNotificationService.sendNotificationEmail(saved);
        } catch (Exception e) {
            log.warn("Failed to queue email notification: {}", e.getMessage());
        }

        if (recipient.isPushNotifications()) {
            NotificationDTO dto = toDTO(saved);
            eventPublisher.publishEvent(new NotificationCreatedEvent(recipient.getId(), dto));
        }

        return saved;
    }

    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .senderFirstName(notification.getSender() != null ? notification.getSender().getFirstName() : null)
                .senderLastName(notification.getSender() != null ? notification.getSender().getLastName() : null)
                .type(notification.getType())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
