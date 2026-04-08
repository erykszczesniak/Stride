package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final EmailNotificationService emailNotificationService;

    @Scheduled(cron = "0 0 9 ? * MON")
    @Transactional(readOnly = true)
    public void sendWeeklySummaries() {
        log.info("Starting weekly summary email job");

        List<User> users = userRepository.findAll();
        int sent = 0;

        for (User user : users) {
            if (!user.isWeeklySummary() || !user.isActive()) {
                continue;
            }

            try {
                sendSummaryForUser(user);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send weekly summary for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Weekly summary job completed. Sent {} emails.", sent);
    }

    private void sendSummaryForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfLastWeek = now.with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                .minusWeeks(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfLastWeek = startOfLastWeek.plusWeeks(1).minusSeconds(1);

        List<Activity> activities = activityRepository.findByUserIdAndDateRange(
                user.getId(), startOfLastWeek, endOfLastWeek);

        double totalDistance = activities.stream()
                .mapToDouble(a -> a.getDistance() != null ? a.getDistance() : 0.0)
                .sum();
        long totalDuration = activities.stream()
                .mapToLong(a -> a.getDuration() != null ? a.getDuration() : 0L)
                .sum();
        int totalActivities = activities.size();
        int totalElevation = activities.stream()
                .mapToInt(a -> a.getElevationGain() != null ? a.getElevationGain() : 0)
                .sum();
        int totalCalories = activities.stream()
                .mapToInt(a -> a.getCalories() != null ? a.getCalories() : 0)
                .sum();

        String weekLabel = startOfLastWeek.format(DateTimeFormatter.ofPattern("MMM d"))
                + " - " + endOfLastWeek.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));

        String subject = "Stride - Your Weekly Training Summary (" + weekLabel + ")";
        String htmlBody = buildSummaryHtml(user, weekLabel, totalDistance, totalDuration,
                totalActivities, totalElevation, totalCalories);

        emailNotificationService.sendWeeklySummaryEmail(user, subject, htmlBody);
    }

    private String buildSummaryHtml(User user, String weekLabel, double totalDistance,
                                     long totalDuration, int totalActivities,
                                     int totalElevation, int totalCalories) {
        String hours = String.valueOf(totalDuration / 3600);
        String minutes = String.valueOf((totalDuration % 3600) / 60);

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
                      <td style="background:linear-gradient(135deg,#f97316,#ea580c);padding:24px 32px;border-radius:12px 12px 0 0;">
                        <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:700;">Stride</h1>
                        <p style="color:#fed7aa;margin:8px 0 0;font-size:14px;">Weekly Training Summary</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#ffffff;padding:32px;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                        <p style="color:#1e293b;font-size:18px;font-weight:600;margin:0 0 4px;">
                          Hi %s! &#128075;
                        </p>
                        <p style="color:#64748b;font-size:14px;margin:0 0 24px;">
                          Here's your training summary for <strong>%s</strong>
                        </p>

                        <table width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                          <tr>
                            <td width="50%%" style="padding:12px;background:#fff7ed;border-radius:8px;text-align:center;">
                              <div style="font-size:28px;font-weight:700;color:#ea580c;">%.1f km</div>
                              <div style="font-size:12px;color:#9a3412;text-transform:uppercase;letter-spacing:1px;margin-top:4px;">Distance</div>
                            </td>
                            <td width="8"></td>
                            <td width="50%%" style="padding:12px;background:#eff6ff;border-radius:8px;text-align:center;">
                              <div style="font-size:28px;font-weight:700;color:#2563eb;">%sh %sm</div>
                              <div style="font-size:12px;color:#1e40af;text-transform:uppercase;letter-spacing:1px;margin-top:4px;">Time</div>
                            </td>
                          </tr>
                          <tr><td colspan="3" height="8"></td></tr>
                          <tr>
                            <td width="50%%" style="padding:12px;background:#f0fdf4;border-radius:8px;text-align:center;">
                              <div style="font-size:28px;font-weight:700;color:#16a34a;">%d</div>
                              <div style="font-size:12px;color:#166534;text-transform:uppercase;letter-spacing:1px;margin-top:4px;">Activities</div>
                            </td>
                            <td width="8"></td>
                            <td width="50%%" style="padding:12px;background:#fef2f2;border-radius:8px;text-align:center;">
                              <div style="font-size:28px;font-weight:700;color:#dc2626;">%d kcal</div>
                              <div style="font-size:12px;color:#991b1b;text-transform:uppercase;letter-spacing:1px;margin-top:4px;">Calories</div>
                            </td>
                          </tr>
                          <tr><td colspan="3" height="8"></td></tr>
                          <tr>
                            <td colspan="3" style="padding:12px;background:#faf5ff;border-radius:8px;text-align:center;">
                              <div style="font-size:28px;font-weight:700;color:#7c3aed;">%d m</div>
                              <div style="font-size:12px;color:#5b21b6;text-transform:uppercase;letter-spacing:1px;margin-top:4px;">Elevation Gain</div>
                            </td>
                          </tr>
                        </table>

                        <p style="color:#64748b;font-size:14px;margin:0;text-align:center;">
                          %s
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;padding:20px 32px;border-radius:0 0 12px 12px;border-top:1px solid #e2e8f0;">
                        <p style="color:#94a3b8;font-size:12px;margin:0;text-align:center;">
                          You're receiving this because you have weekly summaries enabled. Manage your preferences in Settings.
                        </p>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                user.getFirstName(),
                weekLabel,
                totalDistance,
                hours,
                minutes,
                totalActivities,
                totalCalories,
                totalElevation,
                totalActivities > 0
                        ? "Keep up the great work! Every session counts."
                        : "No activities recorded last week. Get out there and crush it this week!"
        );
    }
}
