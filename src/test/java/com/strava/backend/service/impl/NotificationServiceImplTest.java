package com.strava.backend.service.impl;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;
import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.enums.NotificationType;
import com.strava.backend.event.NotificationCreatedEvent;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.NotificationRepository;
import com.strava.backend.service.EmailNotificationService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User recipient;
    private User sender;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        recipient = TestDataFactory.createUser(1L, "recipient@example.com", "Alice", "Smith");
        sender = TestDataFactory.createUser(2L, "sender@example.com", "Bob", "Jones");

        testNotification = Notification.builder()
                .id(1L)
                .recipient(recipient)
                .sender(sender)
                .type(NotificationType.LIKE)
                .message("Bob liked your activity")
                .referenceId(100L)
                .referenceType("ACTIVITY")
                .isRead(false)
                .build();
    }

    @Nested
    @DisplayName("createNotification Tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification successfully")
        void shouldCreateNotificationSuccessfully() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            Notification result = notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getMessage()).isEqualTo("Bob liked your activity");
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should skip self-notification when sender equals recipient")
        void shouldSkipSelfNotification() {
            Notification result = notificationService.createNotification(
                    recipient, recipient, NotificationType.LIKE, "You liked your activity", 100L, "ACTIVITY");

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip duplicate unread LIKE notification for same activity")
        void shouldSkipDuplicateUnreadLikeNotification() {
            when(notificationRepository.existsByRecipientIdAndSenderIdAndTypeAndReferenceIdAndIsReadFalse(
                    1L, 2L, NotificationType.LIKE, 100L)).thenReturn(true);

            Notification result = notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow duplicate COMMENT notification for same activity")
        void shouldAllowDuplicateCommentNotification() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            Notification result = notificationService.createNotification(
                    recipient, sender, NotificationType.COMMENT, "Bob commented on your activity", 100L, "ACTIVITY");

            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should publish SSE event when recipient has pushNotifications enabled")
        void shouldPublishSseEventWhenPushNotificationsEnabled() {
            recipient.setPushNotifications(true);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            ArgumentCaptor<NotificationCreatedEvent> eventCaptor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getRecipientId()).isEqualTo(1L);
            assertThat(eventCaptor.getValue().getNotification()).isNotNull();
        }

        @Test
        @DisplayName("Should NOT publish SSE event when pushNotifications disabled")
        void shouldNotPublishSseEventWhenPushNotificationsDisabled() {
            recipient.setPushNotifications(false);
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
        }

        @Test
        @DisplayName("Should call emailNotificationService.sendNotificationEmail")
        void shouldCallEmailNotificationService() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            verify(emailNotificationService).sendNotificationEmail(testNotification);
        }

        @Test
        @DisplayName("Should not fail when email sending throws exception")
        void shouldNotFailWhenEmailSendingThrowsException() {
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
            doThrow(new RuntimeException("Email error")).when(emailNotificationService).sendNotificationEmail(any());

            Notification result = notificationService.createNotification(
                    recipient, sender, NotificationType.LIKE, "Bob liked your activity", 100L, "ACTIVITY");

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should create notification with null sender")
        void shouldCreateNotificationWithNullSender() {
            Notification notificationWithNullSender = Notification.builder()
                    .id(2L)
                    .recipient(recipient)
                    .sender(null)
                    .type(NotificationType.SYSTEM)
                    .message("System notification")
                    .referenceId(null)
                    .referenceType(null)
                    .isRead(false)
                    .build();
            when(notificationRepository.save(any(Notification.class))).thenReturn(notificationWithNullSender);

            Notification result = notificationService.createNotification(
                    recipient, null, NotificationType.SYSTEM, "System notification", null, null);

            assertThat(result).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("markAsRead Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            notificationService.markAsRead(1L, 1L);

            assertThat(testNotification.isRead()).isTrue();
            verify(notificationRepository).save(testNotification);
        }

        @Test
        @DisplayName("Should throw 404 for non-existent notification")
        void shouldThrow404ForNonExistentNotification() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Notification not found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw 403 when user tries to mark someone else's notification")
        void shouldThrow403WhenNotRecipient() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

            assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Access denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("markAllAsRead Tests")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should call repository markAllAsReadByRecipientId")
        void shouldCallRepositoryMarkAllAsRead() {
            notificationService.markAllAsRead(1L);

            verify(notificationRepository).markAllAsReadByRecipientId(1L);
        }
    }

    @Nested
    @DisplayName("deleteNotification Tests")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification successfully")
        void shouldDeleteNotificationSuccessfully() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

            notificationService.deleteNotification(1L, 1L);

            verify(notificationRepository).delete(testNotification);
        }

        @Test
        @DisplayName("Should throw 404 for non-existent notification")
        void shouldThrow404ForNonExistentNotification() {
            when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.deleteNotification(999L, 1L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Notification not found")
                    .extracting("status")
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw 403 when deleting someone else's notification")
        void shouldThrow403WhenNotRecipient() {
            when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

            assertThatThrownBy(() -> notificationService.deleteNotification(1L, 99L))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage("Access denied")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("clearAllNotifications Tests")
    class ClearAllNotificationsTests {

        @Test
        @DisplayName("Should call repository deleteAllByRecipientId")
        void shouldCallRepositoryDeleteAll() {
            notificationService.clearAllNotifications(1L);

            verify(notificationRepository).deleteAllByRecipientId(1L);
        }
    }

    @Nested
    @DisplayName("getNotifications Tests")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return list of notification DTOs")
        void shouldReturnNotificationDTOs() {
            when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(testNotification));

            List<NotificationDTO> result = notificationService.getNotifications(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMessage()).isEqualTo("Bob liked your activity");
            assertThat(result.get(0).getSenderFirstName()).isEqualTo("Bob");
            assertThat(result.get(0).getSenderLastName()).isEqualTo("Jones");
            assertThat(result.get(0).getType()).isEqualTo(NotificationType.LIKE);
        }

        @Test
        @DisplayName("Should return empty list when no notifications")
        void shouldReturnEmptyList() {
            when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            List<NotificationDTO> result = notificationService.getNotifications(1L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadCount Tests")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return unread notification count")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(5L);

            long result = notificationService.getUnreadCount(1L);

            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnread() {
            when(notificationRepository.countByRecipientIdAndIsReadFalse(1L)).thenReturn(0L);

            long result = notificationService.getUnreadCount(1L);

            assertThat(result).isEqualTo(0L);
        }
    }
}
