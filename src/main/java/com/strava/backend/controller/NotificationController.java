package com.strava.backend.controller;

import com.strava.backend.dto.NotificationDTO;
import com.strava.backend.security.UserPrincipal;
import com.strava.backend.service.NotificationService;
import com.strava.backend.service.WeeklySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.strava.backend.constants.PathConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(API_V1_NOTIFICATIONS)
public class NotificationController {

    private final NotificationService notificationService;
    private final WeeklySummaryService weeklySummaryService;

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getId();
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(notificationService.getNotifications(currentUserId));
    }

    @GetMapping(UNREAD_COUNT)
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long currentUserId = getAuthenticatedUserId();
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(currentUserId)));
    }

    @PutMapping(NOTIFICATION_ID + READ)
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long notificationId) {
        Long currentUserId = getAuthenticatedUserId();
        notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping(READ_ALL)
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        Long currentUserId = getAuthenticatedUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping(NOTIFICATION_ID)
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long notificationId) {
        Long currentUserId = getAuthenticatedUserId();
        notificationService.deleteNotification(notificationId, currentUserId);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearAllNotifications() {
        Long currentUserId = getAuthenticatedUserId();
        notificationService.clearAllNotifications(currentUserId);
        return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
    }

    @PostMapping("/weekly-summary/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> triggerWeeklySummary() {
        weeklySummaryService.sendWeeklySummaries();
        return ResponseEntity.ok(Map.of("message", "Weekly summary triggered"));
    }
}
