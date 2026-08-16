package com.strava.webbff.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.strava.webbff.client.ActivityClient;
import com.strava.webbff.client.AdminPage;
import com.strava.webbff.client.EventClient;
import com.strava.webbff.client.GoalClient;
import com.strava.webbff.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Composes the monolith's {@code AdminServiceImpl#getAdminStats} from the
 * public admin/list surfaces of the owning services (there is no shared
 * database to COUNT() against anymore):
 *
 * <ul>
 *   <li>{@code totalUsers} / {@code totalActivities} — exact, from the
 *       page-total-elements totals of the admin list envelopes;</li>
 *   <li>{@code activeUsers}, {@code newUsersThisWeek}, {@code activitiesThisWeek},
 *       {@code activitiesThisMonth}, {@code totalDistanceKm} — derived by
 *       scanning the admin lists newest-first ({@value #SCAN_PAGE_SIZE} rows a
 *       page, capped at {@value #MAX_SCAN_PAGES} pages). Exact while the
 *       dataset fits in the cap (2000 rows); beyond it the derived values
 *       cover the newest rows only — a deliberate trade-off documented in the
 *       phase notes, exact fidelity would need internal count endpoints;</li>
 *   <li>{@code totalClubs} / {@code totalChallenges} / {@code totalEvents} —
 *       approximated by the sizes of the club / active-challenge / upcoming-
 *       public-event lists (first {@value #SCAN_PAGE_SIZE}); the monolith
 *       counted all rows including private/past ones, which no public
 *       endpoint exposes.</li>
 * </ul>
 *
 * Every source degrades to zero via the clients' Resilience4j fallbacks, so
 * the admin console renders even with a downstream outage.
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    static final int SCAN_PAGE_SIZE = 200;
    static final int MAX_SCAN_PAGES = 10;

    private final UserClient userClient;
    private final ActivityClient activityClient;
    private final GoalClient goalClient;
    private final EventClient eventClient;
    private final Clock clock;

    public AdminStatsResponse getAdminStats() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();

        UserScan users = scanUsers(oneWeekAgo);
        ActivityScan activities = scanActivities(oneWeekAgo, startOfMonth);

        return AdminStatsResponse.builder()
                .totalUsers(users.totalElements)
                .activeUsers(users.active)
                .totalActivities(activities.totalElements)
                .totalClubs(eventClient.clubs(SCAN_PAGE_SIZE).size())
                .totalChallenges(goalClient.activeChallenges(SCAN_PAGE_SIZE).size())
                .totalEvents(eventClient.upcomingPublicEvents(SCAN_PAGE_SIZE).size())
                .newUsersThisWeek(users.createdThisWeek)
                .activitiesThisWeek(activities.createdThisWeek)
                .totalDistanceKm(activities.totalDistanceKm)
                .activitiesThisMonth(activities.createdThisMonth)
                .build();
    }

    private UserScan scanUsers(LocalDateTime oneWeekAgo) {
        UserScan scan = new UserScan();
        for (int page = 0; page < MAX_SCAN_PAGES; page++) {
            AdminPage adminPage = userClient.adminUsers(page, SCAN_PAGE_SIZE);
            if (page == 0) {
                scan.totalElements = adminPage.totalElements();
            }
            for (JsonNode user : adminPage.items()) {
                if (user.path("active").asBoolean()) {
                    scan.active++;
                }
                LocalDateTime createdAt = parse(user.path("createdAt").asText(null));
                if (createdAt != null && !createdAt.isBefore(oneWeekAgo)) {
                    scan.createdThisWeek++;
                }
            }
            if (adminPage.items().size() < SCAN_PAGE_SIZE) {
                break;
            }
        }
        return scan;
    }

    private ActivityScan scanActivities(LocalDateTime oneWeekAgo, LocalDateTime startOfMonth) {
        ActivityScan scan = new ActivityScan();
        for (int page = 0; page < MAX_SCAN_PAGES; page++) {
            AdminPage adminPage = activityClient.adminActivities(page, SCAN_PAGE_SIZE);
            if (page == 0) {
                scan.totalElements = adminPage.totalElements();
            }
            for (JsonNode activity : adminPage.items()) {
                scan.totalDistanceKm += activity.path("distance").asDouble(0.0);
                LocalDateTime createdAt = parse(activity.path("createdAt").asText(null));
                if (createdAt != null && !createdAt.isBefore(oneWeekAgo)) {
                    scan.createdThisWeek++;
                }
                if (createdAt != null && !createdAt.isBefore(startOfMonth)) {
                    scan.createdThisMonth++;
                }
            }
            if (adminPage.items().size() < SCAN_PAGE_SIZE) {
                break;
            }
        }
        return scan;
    }

    private static LocalDateTime parse(String isoDateTime) {
        return isoDateTime != null ? LocalDateTime.parse(isoDateTime) : null;
    }

    private static final class UserScan {
        long totalElements;
        long active;
        long createdThisWeek;
    }

    private static final class ActivityScan {
        long totalElements;
        long createdThisWeek;
        long createdThisMonth;
        // COALESCE(SUM(distance), 0) in main — starts at 0.0, never null.
        double totalDistanceKm;
    }
}
