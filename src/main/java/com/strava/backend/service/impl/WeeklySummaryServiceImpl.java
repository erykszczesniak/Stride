package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.EmailNotificationService;
import com.strava.backend.service.WeeklySummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySummaryServiceImpl implements WeeklySummaryService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final EmailNotificationService emailNotificationService;
    private final TemplateEngine templateEngine;

    @Value("${app.name:Stride}")
    private String appName;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
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

        String subject = appName + " - Your Weekly Training Summary (" + weekLabel + ")";

        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("firstName", user.getFirstName());
        context.setVariable("weekLabel", weekLabel);
        context.setVariable("distance", String.format("%.1f", totalDistance));
        context.setVariable("hours", String.valueOf(totalDuration / 3600));
        context.setVariable("minutes", String.valueOf((totalDuration % 3600) / 60));
        context.setVariable("activities", totalActivities);
        context.setVariable("calories", totalCalories);
        context.setVariable("elevation", totalElevation);
        context.setVariable("motivationMessage", totalActivities > 0
                ? "Keep up the great work! Every session counts."
                : "No activities recorded last week. Get out there and crush it this week!");
        context.setVariable("settingsUrl", allowedOrigins.split(",")[0].trim() + "/settings?section=notifications");

        String htmlBody = templateEngine.process("mail/weekly-summary", context);

        emailNotificationService.sendWeeklySummaryEmail(user, subject, htmlBody);
    }
}
