package com.strava.backend.service;

import com.strava.backend.domain.Notification;
import com.strava.backend.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.name:Stride}")
    private String appName;

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

    private String buildHtmlBody(Notification notification) {
        String senderName = notification.getSender() != null
                ? notification.getSender().getFullName()
                : appName;

        String typeColor = switch (notification.getType()) {
            case LIKE -> "#ef4444";
            case COMMENT -> "#3b82f6";
            case FOLLOW -> "#8b5cf6";
            case ACHIEVEMENT -> "#f59e0b";
            case GOAL_COMPLETED -> "#10b981";
            case SYSTEM -> "#6b7280";
        };

        String typeIcon = switch (notification.getType()) {
            case LIKE -> "&#10084;&#65039;";
            case COMMENT -> "&#128172;";
            case FOLLOW -> "&#128100;";
            case ACHIEVEMENT -> "&#127942;";
            case GOAL_COMPLETED -> "&#127919;";
            case SYSTEM -> "&#128276;";
        };

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background-color:#f3f4f6;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;padding:20px;">
                    <tr>
                      <td style="background:linear-gradient(135deg,%s,#1e293b);padding:24px 32px;border-radius:12px 12px 0 0;">
                        <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;">%s</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#ffffff;padding:32px;border-radius:0 0 12px 12px;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <div style="font-size:32px;margin-bottom:16px;">%s</div>
                        <p style="color:#1e293b;font-size:16px;line-height:1.6;margin:0 0 16px;">
                          %s
                        </p>
                        <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                        <p style="color:#9ca3af;font-size:13px;margin:0;">
                          Hi %s, this notification was sent from %s. You can manage your notification preferences in Settings.
                        </p>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                typeColor,
                appName,
                typeIcon,
                notification.getMessage(),
                notification.getRecipient().getFirstName(),
                appName
        );
    }
}
