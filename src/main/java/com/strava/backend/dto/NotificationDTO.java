package com.strava.backend.dto;

import com.strava.backend.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String senderFirstName;
    private String senderLastName;
    private NotificationType type;
    private String message;
    private Long referenceId;
    private String referenceType;
    private boolean isRead;
    private LocalDateTime createdAt;
}
