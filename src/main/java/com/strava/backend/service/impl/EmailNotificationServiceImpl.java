package com.strava.backend.service.impl;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;
import com.strava.backend.service.EmailNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.name:Stride}")
    private String appName;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    @Async
    public void sendNotificationEmail(Notification notification) {
        User recipient = notification.getRecipient();

        if (!recipient.isEmailNotifications()) {
            return;
        }

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail username not configured, skipping email notification for user {}", recipient.getId());
            return;
        }

        String subject = getSubject(notification);
        String body = buildHtmlBody(notification);

        sendHtmlEmail(recipient.getEmail(), subject, body);
    }

    @Override
    @Async
    public void sendWeeklySummaryEmail(User recipient, String subject, String htmlBody) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Mail username not configured, skipping weekly summary for user {}", recipient.getId());
            return;
        }

        sendHtmlEmail(recipient.getEmail(), subject, htmlBody);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
        }
    }

    private String getSubject(Notification notification) {
        return switch (notification.getType()) {
            case LIKE -> appName + " - Someone liked your activity";
            case COMMENT -> appName + " - New comment on your activity";
            case FOLLOW -> appName + " - You have a new follower";
            case ACHIEVEMENT -> appName + " - Achievement unlocked!";
            case GOAL_COMPLETED -> appName + " - Goal completed!";
            case SYSTEM -> appName + " - System notification";
        };
    }

    private String getFrontendUrl() {
        return allowedOrigins.split(",")[0].trim();
    }

    private String buildActionUrl(Notification notification) {
        String base = getFrontendUrl();
        if (notification.getReferenceId() == null) return base;

        return switch (notification.getReferenceType()) {
            case "ACTIVITY" -> base + "/activities/" + notification.getReferenceId();
            case "GOAL" -> base + "/goals/" + notification.getReferenceId();
            default -> base;
        };
    }

    private String getActionLabel(Notification notification) {
        return switch (notification.getType()) {
            case LIKE, COMMENT -> "View Activity";
            case FOLLOW -> "View Profile";
            case GOAL_COMPLETED -> "View Goal";
            default -> "Open " + appName;
        };
    }

    private String getTypeIcon(Notification notification) {
        return switch (notification.getType()) {
            case LIKE -> "&#10084;&#65039;";
            case COMMENT -> "&#128172;";
            case FOLLOW -> "&#128100;";
            case ACHIEVEMENT -> "&#127942;";
            case GOAL_COMPLETED -> "&#127919;";
            case SYSTEM -> "&#128276;";
        };
    }

    private String buildHtmlBody(Notification notification) {
        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("icon", getTypeIcon(notification));
        context.setVariable("message", notification.getMessage());
        context.setVariable("actionUrl", buildActionUrl(notification));
        context.setVariable("actionLabel", getActionLabel(notification));
        context.setVariable("recipientName", notification.getRecipient().getFirstName());
        context.setVariable("settingsUrl", getFrontendUrl() + "/settings?section=notifications");

        return templateEngine.process("mail/notification", context);
    }
}
