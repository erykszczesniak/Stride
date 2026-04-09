package com.strava.backend.service;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;

public interface EmailNotificationService {

    void sendNotificationEmail(Notification notification);

    void sendWeeklySummaryEmail(User recipient, String subject, String htmlBody);
}
