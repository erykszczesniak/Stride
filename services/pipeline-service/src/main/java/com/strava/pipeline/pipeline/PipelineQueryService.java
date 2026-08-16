package com.strava.pipeline.pipeline;

import com.strava.pipeline.client.ActivityClient;
import com.strava.pipeline.client.EventClient;
import com.strava.pipeline.client.GoalClient;
import com.strava.pipeline.client.UserClient;
import com.strava.pipeline.exception.ApiRequestException;
import com.strava.pipeline.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only facade assembling the pipeline contract (the triathlon-tracker
 * client's API) — ported from main with the repository/domain-service data
 * access swapped for REST composition over activity-service, user-service,
 * challenge-goal-service and club-event-service. All queries are scoped to
 * the token owner, with one exception: activity detail also serves activities
 * the owner is allowed to view (public activity of a public or followed
 * profile), so feed tap-throughs open — that rule is enforced by
 * activity-service's ActivityVisibility, which 404s denied ids exactly like
 * main's resolveViewableActivity did. Private data of other users is never
 * exposed.
 */
@Service
@RequiredArgsConstructor
public class PipelineQueryService {

    private static final int RECENT_ACTIVITIES = 3;
    private static final int NEW_PR_WINDOW_DAYS = 14;
    private static final int WEEKLY_VOLUME_WEEKS = 12;

    private final ActivityClient activityClient;
    private final UserClient userClient;
    private final GoalClient goalClient;
    private final EventClient eventClient;
    private final ActivityStreamAnalyzer streamAnalyzer;
    private final TrainingLoadCalculator loadCalculator;

    // --- activities ------------------------------------------------------

    public List<PipelineActivityDto> listActivities(Long userId) {
        return mappableActivities(userId).stream()
                .map(this::toActivityDto)
                .toList();
    }

    public PipelineActivityDetailDto activityDetail(Long userId, String pipelineId) {
        ActivityData activity = resolveViewableActivity(userId, pipelineId);

        List<ActivityStreamAnalyzer.SplitSegment> segments = streamAnalyzer.computeSplits(activity);
        List<PipelineActivityDetailDto.Split> splits = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            ActivityStreamAnalyzer.SplitSegment segment = segments.get(i);
            splits.add(new PipelineActivityDetailDto.Split(
                    segment.distanceMeters(),
                    segment.durationSeconds(),
                    streamAnalyzer.splitHeartRate(activity, i, segments.size())));
        }

        return new PipelineActivityDetailDto(
                toActivityDto(activity),
                streamAnalyzer.paceSeries(activity),
                streamAnalyzer.heartRateSeries(activity),
                splits,
                streamAnalyzer.encodedRoutePolyline(activity),
                streamAnalyzer.elevationSeries(activity));
    }

    // --- dashboard -------------------------------------------------------

    public PipelineDashboardDto dashboard(Long userId) {
        List<ActivityData> activities = mappableActivities(userId);

        LocalDate today = LocalDate.now();
        Map<LocalDate, Double> dailyTss = loadCalculator.dailyTss(activities);
        List<TrainingLoadCalculator.DayLoad> series =
                loadCalculator.loadSeries(dailyTss, today, today);
        TrainingLoadCalculator.DayLoad current = series.get(series.size() - 1);

        PipelineDashboardDto.Form form = new PipelineDashboardDto.Form(
                OffsetDateTime.now(),
                (int) Math.round(current.ctl()),
                (int) Math.round(current.atl()),
                (int) Math.round(current.tsb()));

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<PipelineDashboardDto.SportSnapshot> snapshots = new ArrayList<>();
        for (String discipline : List.of("swim", "bike", "run")) {
            double thisWeek = weekDistanceKm(activities, discipline, weekStart);
            double lastWeek = weekDistanceKm(activities, discipline, weekStart.minusWeeks(1));
            snapshots.add(new PipelineDashboardDto.SportSnapshot(
                    discipline, round1(thisWeek), trendPercent(thisWeek, lastWeek)));
        }

        List<PipelineActivityDto> recent = activities.stream()
                .limit(RECENT_ACTIVITIES)
                .map(this::toActivityDto)
                .toList();

        // Stride has no per-day planned workouts — null renders the rest-day card.
        return new PipelineDashboardDto(form, null, snapshots, recent);
    }

    // --- training load ---------------------------------------------------

    public PipelineLoadDto load(Long userId, String range) {
        String normalized = switch (range == null ? "4w" : range) {
            case "3m" -> "3m";
            case "1y" -> "1y";
            default -> "4w";
        };
        int rangeDays = switch (normalized) {
            case "3m" -> 91;
            case "1y" -> 364;
            default -> 28;
        };
        int sampleEveryDays = switch (normalized) {
            case "3m" -> 7;
            case "1y" -> 28;
            default -> 4;
        };

        List<ActivityData> activities = mappableActivities(userId);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(rangeDays);
        Map<LocalDate, Double> dailyTss = loadCalculator.dailyTss(activities);
        List<TrainingLoadCalculator.DayLoad> series = loadCalculator.loadSeries(dailyTss, from, today);

        List<PipelineLoadDto.MetricPoint> ctl = new ArrayList<>();
        List<PipelineLoadDto.MetricPoint> atl = new ArrayList<>();
        for (int i = 0; i < series.size(); i++) {
            TrainingLoadCalculator.DayLoad day = series.get(i);
            boolean lastPoint = i == series.size() - 1;
            if (i % sampleEveryDays == 0 || lastPoint) {
                OffsetDateTime date = day.date().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
                ctl.add(new PipelineLoadDto.MetricPoint(date, round1(day.ctl())));
                atl.add(new PipelineLoadDto.MetricPoint(date, round1(day.atl())));
            }
        }

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<PipelineLoadDto.WeeklyVolume> volumes = new ArrayList<>();
        for (int w = WEEKLY_VOLUME_WEEKS - 1; w >= 0; w--) {
            LocalDate start = weekStart.minusWeeks(w);
            volumes.add(new PipelineLoadDto.WeeklyVolume(
                    start.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
                    round1(weekDistanceKm(activities, "swim", start)),
                    round1(weekDistanceKm(activities, "bike", start)),
                    round1(weekDistanceKm(activities, "run", start))));
        }

        int thisWeekSeconds = weekDurationSeconds(activities, weekStart);
        int lastWeekSeconds = weekDurationSeconds(activities, weekStart.minusWeeks(1));
        int thisWeekTss = weekTss(activities, weekStart);
        int lastWeekTss = weekTss(activities, weekStart.minusWeeks(1));
        PipelineLoadDto.WeekSummary summary = new PipelineLoadDto.WeekSummary(
                thisWeekSeconds,
                thisWeekSeconds - lastWeekSeconds,
                thisWeekTss,
                thisWeekTss - lastWeekTss);

        return new PipelineLoadDto(normalized, ctl, atl, volumes, summary);
    }

    // --- records ---------------------------------------------------------

    public PipelineRecordsDto records(Long userId) {
        LocalDateTime newSince = LocalDateTime.now().minusDays(NEW_PR_WINDOW_DAYS);
        List<PipelineRecordsDto.RecordItem> items = activityClient.personalRecords(userId).stream()
                .filter(pr -> discipline(pr.activityType()) != null)
                .sorted(Comparator
                        .comparingInt((RecordData pr) -> disciplineOrder(discipline(pr.activityType())))
                        .thenComparing(pr -> pr.distanceType().getDistanceKm()))
                .map(pr -> toRecordItem(pr, newSince))
                .toList();
        return new PipelineRecordsDto(items);
    }

    private PipelineRecordsDto.RecordItem toRecordItem(RecordData pr, LocalDateTime newSince) {
        Integer previous = pr.previousTimeInSeconds();
        List<Double> progression = previous != null
                ? List.of((double) previous, (double) pr.timeInSeconds())
                : List.of((double) pr.timeInSeconds());
        return new PipelineRecordsDto.RecordItem(
                "rec-" + pr.id(),
                discipline(pr.activityType()),
                eventLabel(pr.distanceType()),
                PipelineRecordsDto.Value.ofSeconds(pr.timeInSeconds()),
                toOffset(pr.achievedAt()),
                pr.achievedAt() != null && pr.achievedAt().isAfter(newSince),
                previous != null ? PipelineRecordsDto.Value.ofSeconds(previous) : null,
                progression);
    }

    // --- profile ---------------------------------------------------------

    public PipelineProfileDto profile(Long userId) {
        UserClient.UserData user = userClient.me(userId);

        String firstName = user.firstName() == null ? "" : user.firstName();
        String lastName = user.lastName() == null ? "" : user.lastName();
        String initials = (initial(firstName) + initial(lastName)).toUpperCase();
        PipelineProfileDto.Athlete athlete = new PipelineProfileDto.Athlete(
                (firstName + " " + lastName).trim(),
                initials.isEmpty() ? "?" : initials,
                // Stride's user model has no birth date, so no age-group bands.
                "OPEN",
                user.city() == null ? "" : user.city());

        List<ActivityData> activities = mappableActivities(userId);
        LocalDateTime seasonStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        List<ActivityData> season = activities.stream()
                .filter(a -> a.startTime() != null && !a.startTime().isBefore(seasonStart))
                .toList();
        List<RecordData> records = activityClient.personalRecords(userId);
        int prsThisSeason = (int) records.stream()
                .filter(pr -> pr.achievedAt() != null && !pr.achievedAt().isBefore(seasonStart))
                .count();
        PipelineProfileDto.SeasonStats seasonStats = new PipelineProfileDto.SeasonStats(
                season.size(),
                season.stream().mapToInt(a -> a.duration() == null ? 0 : a.duration()).sum(),
                prsThisSeason,
                round1(disciplineKm(season, "swim")),
                round1(disciplineKm(season, "bike")),
                round1(disciplineKm(season, "run")));

        return new PipelineProfileDto(athlete, seasonStats, seasonGoal(userId), zones(records, activities));
    }

    private PipelineProfileDto.SeasonGoal seasonGoal(Long userId) {
        Optional<EventClient.EventData> nextEvent = eventClient.myUpcomingEvents(userId).stream().findFirst();
        int targetSeconds = goalClient.activeGoals(userId).stream()
                .filter(g -> "TIME".equals(g.goalType()) && g.targetValue() != null)
                .findFirst()
                // Goal.targetValue stores time goals in hours.
                .map(g -> (int) Math.round(g.targetValue() * 3600))
                .orElse(0);
        return nextEvent
                .map(event -> new PipelineProfileDto.SeasonGoal(
                        event.name(),
                        toOffset(event.eventDate()),
                        targetSeconds))
                .orElseGet(() -> new PipelineProfileDto.SeasonGoal(
                        "Season " + LocalDate.now().getYear(),
                        LocalDate.now().withMonth(12).withDayOfMonth(31)
                                .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime(),
                        targetSeconds));
    }

    /**
     * Threshold zones derived from personal records where possible:
     * run threshold ≈ 10K race pace (or 5K pace + 4%), swim CSS ≈ 400 m PR
     * pace per 100 m, FTP ≈ 95% of the best ride average power. Falls back to
     * recent-activity average pace, then 0 ("not set") when nothing applies.
     */
    private PipelineProfileDto.ThresholdZones zones(List<RecordData> records, List<ActivityData> activities) {
        int thresholdPace = prTime(records, ActivityType.RUN, DistanceType.TEN_KM)
                .map(t -> t / 10)
                .or(() -> prTime(records, ActivityType.RUN, DistanceType.FIVE_KM)
                        .map(t -> (int) Math.round(t / 5.0 * 1.04)))
                .orElseGet(() -> (int) Math.round(averagePaceSecondsPerKm(activities, "run")));

        int css = prTime(records, ActivityType.SWIM, DistanceType.FOUR_HUNDRED_M)
                .map(t -> t / 4)
                .or(() -> prTime(records, ActivityType.SWIM, DistanceType.ONE_KM).map(t -> t / 10))
                .orElseGet(() -> (int) Math.round(averagePaceSecondsPerKm(activities, "swim") / 10));

        // The public list payload carries no power — enrich rides through the
        // internal batch API, then apply main's formula unchanged.
        List<Long> bikeIds = activities.stream()
                .filter(a -> "bike".equals(discipline(a.activityType())))
                .map(ActivityData::id)
                .toList();
        Map<Long, Integer> powerById = activityClient.averagePowerByIds(bikeIds);
        int ftp = bikeIds.stream()
                .map(powerById::get)
                .filter(p -> p != null && p > 0)
                .max(Integer::compareTo)
                .map(p -> (int) Math.round(p * 0.95))
                .orElse(0);

        return new PipelineProfileDto.ThresholdZones(css, ftp, thresholdPace);
    }

    private Optional<Integer> prTime(List<RecordData> records, ActivityType type, DistanceType distance) {
        return records.stream()
                .filter(pr -> pr.activityType() == type && pr.distanceType() == distance)
                .map(RecordData::timeInSeconds)
                .findFirst();
    }

    private double averagePaceSecondsPerKm(List<ActivityData> activities, String discipline) {
        double km = disciplineKm(activities, discipline);
        long seconds = activities.stream()
                .filter(a -> discipline.equals(discipline(a.activityType())))
                .mapToLong(a -> a.duration() == null ? 0 : a.duration())
                .sum();
        return km > 0 ? seconds / km : 0;
    }

    // --- shared helpers --------------------------------------------------

    /** The token owner's activities in the contract's disciplines, newest first. */
    private List<ActivityData> mappableActivities(Long userId) {
        return activityClient.myActivities(userId).stream()
                .filter(a -> discipline(a.activityType()) != null)
                .sorted(Comparator.comparing(ActivityData::startTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ActivityData resolveViewableActivity(Long viewerId, String pipelineId) {
        Long activityId = parsePipelineActivityId(pipelineId);
        ActivityData activity = activityClient.activityDetail(viewerId, activityId);
        if (discipline(activity.activityType()) == null) {
            throw new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND);
        }
        return activity;
    }

    private Long parsePipelineActivityId(String pipelineId) {
        String raw = pipelineId != null && pipelineId.startsWith("act-")
                ? pipelineId.substring(4)
                : pipelineId;
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND);
        }
    }

    private PipelineActivityDto toActivityDto(ActivityData activity) {
        boolean hasGps = GeoUtils.decodePolyline(activity.routePolyline()).size() > 1;
        return new PipelineActivityDto(
                "act-" + activity.id(),
                discipline(activity.activityType()),
                activity.title(),
                toOffset(activity.startTime()),
                activity.duration() == null ? 0 : activity.duration(),
                activity.distance() == null ? 0 : activity.distance(),
                activity.averageHeartRate() == null ? 0 : activity.averageHeartRate(),
                activity.maxHeartRate() == null ? 0 : activity.maxHeartRate(),
                hasGps);
    }

    /** Contract disciplines; anything else is invisible to the pipeline. */
    static String discipline(ActivityType type) {
        return switch (type) {
            case RUN, VIRTUAL_RUN -> "run";
            case RIDE, VIRTUAL_RIDE -> "bike";
            case SWIM -> "swim";
            default -> null;
        };
    }

    private static int disciplineOrder(String discipline) {
        return switch (discipline) {
            case "run" -> 0;
            case "bike" -> 1;
            default -> 2;
        };
    }

    static String eventLabel(DistanceType distanceType) {
        return switch (distanceType) {
            case FIFTY_M -> "50 m";
            case HUNDRED_M -> "100 m";
            case TWO_HUNDRED_M -> "200 m";
            case FOUR_HUNDRED_M -> "400 m";
            case ONE_KM -> "1 km";
            case THREE_KM -> "3 km";
            case FIVE_KM -> "5K";
            case TEN_KM -> "10K";
            case HALF_MARATHON -> "Half Marathon";
            case MARATHON -> "Marathon";
        };
    }

    private double weekDistanceKm(List<ActivityData> activities, String discipline, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return activities.stream()
                .filter(a -> discipline.equals(discipline(a.activityType())))
                .filter(a -> inRange(a.startTime(), from, to))
                .mapToDouble(a -> a.distance() == null ? 0 : a.distance())
                .sum();
    }

    private int weekDurationSeconds(List<ActivityData> activities, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return activities.stream()
                .filter(a -> inRange(a.startTime(), from, to))
                .mapToInt(a -> a.duration() == null ? 0 : a.duration())
                .sum();
    }

    private int weekTss(List<ActivityData> activities, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return (int) Math.round(activities.stream()
                .filter(a -> inRange(a.startTime(), from, to))
                .mapToDouble(loadCalculator::tss)
                .sum());
    }

    private double disciplineKm(List<ActivityData> activities, String discipline) {
        return activities.stream()
                .filter(a -> discipline.equals(discipline(a.activityType())))
                .mapToDouble(a -> a.distance() == null ? 0 : a.distance())
                .sum();
    }

    private static boolean inRange(LocalDateTime value, LocalDateTime fromInclusive, LocalDateTime toExclusive) {
        return value != null && !value.isBefore(fromInclusive) && value.isBefore(toExclusive);
    }

    private static int trendPercent(double current, double previous) {
        if (previous <= 0) {
            return current > 0 ? 100 : 0;
        }
        return (int) Math.round((current - previous) / previous * 100);
    }

    private static String initial(String value) {
        return value.isEmpty() ? "" : value.substring(0, 1);
    }

    private static OffsetDateTime toOffset(LocalDateTime value) {
        return value == null
                ? OffsetDateTime.now()
                : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
