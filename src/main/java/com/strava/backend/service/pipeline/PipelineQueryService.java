package com.strava.backend.service.pipeline;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.Event;
import com.strava.backend.domain.Goal;
import com.strava.backend.domain.PersonalRecord;
import com.strava.backend.domain.User;
import com.strava.backend.dto.pipeline.PipelineActivityDetailDto;
import com.strava.backend.dto.pipeline.PipelineActivityDto;
import com.strava.backend.dto.pipeline.PipelineDashboardDto;
import com.strava.backend.dto.pipeline.PipelineLoadDto;
import com.strava.backend.dto.pipeline.PipelineProfileDto;
import com.strava.backend.dto.pipeline.PipelineRecordsDto;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.DistanceType;
import com.strava.backend.enums.GoalType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.PersonalRecordRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ActivityVisibility;
import com.strava.backend.service.EventService;
import com.strava.backend.service.GoalService;
import com.strava.backend.service.importer.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * client's API, see docs/pipeline-api notes in the PR) from Stride's domain
 * services. All queries are scoped to the token owner, with one exception:
 * activity detail also serves activities the owner is allowed to view
 * (public activity of a public or followed profile), so feed tap-throughs
 * open. Private data of other users is never exposed.
 */
@Service
@RequiredArgsConstructor
public class PipelineQueryService {

    private static final int RECENT_ACTIVITIES = 3;
    private static final int NEW_PR_WINDOW_DAYS = 14;
    private static final int WEEKLY_VOLUME_WEEKS = 12;

    private final ActivityRepository activityRepository;
    private final ActivityVisibility activityVisibility;
    private final PersonalRecordRepository personalRecordRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final GoalService goalService;
    private final ActivityStreamAnalyzer streamAnalyzer;
    private final TrainingLoadCalculator loadCalculator;

    // --- activities ------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PipelineActivityDto> listActivities(Long userId) {
        return mappableActivities(userId).stream()
                .map(this::toActivityDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PipelineActivityDetailDto activityDetail(Long userId, String pipelineId) {
        Activity activity = resolveViewableActivity(userId, pipelineId);

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

    @Transactional(readOnly = true)
    public PipelineDashboardDto dashboard(Long userId) {
        List<Activity> activities = mappableActivities(userId);

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

    @Transactional(readOnly = true)
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

        List<Activity> activities = mappableActivities(userId);
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

    @Transactional(readOnly = true)
    public PipelineRecordsDto records(Long userId) {
        LocalDateTime newSince = LocalDateTime.now().minusDays(NEW_PR_WINDOW_DAYS);
        List<PipelineRecordsDto.RecordItem> items = personalRecordRepository.findByUserId(userId).stream()
                .filter(pr -> discipline(pr.getActivityType()) != null)
                .sorted(Comparator
                        .comparingInt((PersonalRecord pr) -> disciplineOrder(discipline(pr.getActivityType())))
                        .thenComparing(pr -> pr.getDistanceType().getDistanceKm()))
                .map(pr -> toRecordItem(pr, newSince))
                .toList();
        return new PipelineRecordsDto(items);
    }

    private PipelineRecordsDto.RecordItem toRecordItem(PersonalRecord pr, LocalDateTime newSince) {
        Integer previous = pr.getPreviousTimeInSeconds();
        List<Double> progression = previous != null
                ? List.of((double) previous, (double) pr.getTimeInSeconds())
                : List.of((double) pr.getTimeInSeconds());
        return new PipelineRecordsDto.RecordItem(
                "rec-" + pr.getId(),
                discipline(pr.getActivityType()),
                eventLabel(pr.getDistanceType()),
                PipelineRecordsDto.Value.ofSeconds(pr.getTimeInSeconds()),
                toOffset(pr.getAchievedAt()),
                pr.getAchievedAt() != null && pr.getAchievedAt().isAfter(newSince),
                previous != null ? PipelineRecordsDto.Value.ofSeconds(previous) : null,
                progression);
    }

    // --- profile ---------------------------------------------------------

    @Transactional(readOnly = true)
    public PipelineProfileDto profile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException("User not found", HttpStatus.NOT_FOUND));

        String firstName = user.getFirstName() == null ? "" : user.getFirstName();
        String lastName = user.getLastName() == null ? "" : user.getLastName();
        String initials = (initial(firstName) + initial(lastName)).toUpperCase();
        PipelineProfileDto.Athlete athlete = new PipelineProfileDto.Athlete(
                (firstName + " " + lastName).trim(),
                initials.isEmpty() ? "?" : initials,
                // Stride's user model has no birth date, so no age-group bands.
                "OPEN",
                user.getCity() == null ? "" : user.getCity());

        List<Activity> activities = mappableActivities(userId);
        LocalDateTime seasonStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        List<Activity> season = activities.stream()
                .filter(a -> a.getStartTime() != null && !a.getStartTime().isBefore(seasonStart))
                .toList();
        List<PersonalRecord> records = personalRecordRepository.findByUserId(userId);
        int prsThisSeason = (int) records.stream()
                .filter(pr -> pr.getAchievedAt() != null && !pr.getAchievedAt().isBefore(seasonStart))
                .count();
        PipelineProfileDto.SeasonStats seasonStats = new PipelineProfileDto.SeasonStats(
                season.size(),
                season.stream().mapToInt(a -> a.getDuration() == null ? 0 : a.getDuration()).sum(),
                prsThisSeason,
                round1(disciplineKm(season, "swim")),
                round1(disciplineKm(season, "bike")),
                round1(disciplineKm(season, "run")));

        return new PipelineProfileDto(athlete, seasonStats, seasonGoal(userId), zones(records, activities));
    }

    private PipelineProfileDto.SeasonGoal seasonGoal(Long userId) {
        Optional<Event> nextEvent = eventService.getUpcomingEventsByUserId(userId, 1).stream().findFirst();
        int targetSeconds = goalService.getActiveGoalsByUserId(userId).stream()
                .filter(g -> g.getGoalType() == GoalType.TIME && g.getTargetValue() != null)
                .findFirst()
                // Goal.targetValue stores time goals in hours.
                .map(g -> (int) Math.round(g.getTargetValue() * 3600))
                .orElse(0);
        return nextEvent
                .map(event -> new PipelineProfileDto.SeasonGoal(
                        event.getName(),
                        toOffset(event.getEventDate()),
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
    private PipelineProfileDto.ThresholdZones zones(List<PersonalRecord> records, List<Activity> activities) {
        int thresholdPace = prTime(records, ActivityType.RUN, DistanceType.TEN_KM)
                .map(t -> t / 10)
                .or(() -> prTime(records, ActivityType.RUN, DistanceType.FIVE_KM)
                        .map(t -> (int) Math.round(t / 5.0 * 1.04)))
                .orElseGet(() -> (int) Math.round(averagePaceSecondsPerKm(activities, "run")));

        int css = prTime(records, ActivityType.SWIM, DistanceType.FOUR_HUNDRED_M)
                .map(t -> t / 4)
                .or(() -> prTime(records, ActivityType.SWIM, DistanceType.ONE_KM).map(t -> t / 10))
                .orElseGet(() -> (int) Math.round(averagePaceSecondsPerKm(activities, "swim") / 10));

        int ftp = activities.stream()
                .filter(a -> "bike".equals(discipline(a.getActivityType())))
                .map(Activity::getAveragePower)
                .filter(p -> p != null && p > 0)
                .max(Integer::compareTo)
                .map(p -> (int) Math.round(p * 0.95))
                .orElse(0);

        return new PipelineProfileDto.ThresholdZones(css, ftp, thresholdPace);
    }

    private Optional<Integer> prTime(List<PersonalRecord> records, ActivityType type, DistanceType distance) {
        return records.stream()
                .filter(pr -> pr.getActivityType() == type && pr.getDistanceType() == distance)
                .map(PersonalRecord::getTimeInSeconds)
                .findFirst();
    }

    private double averagePaceSecondsPerKm(List<Activity> activities, String discipline) {
        double km = disciplineKm(activities, discipline);
        long seconds = activities.stream()
                .filter(a -> discipline.equals(discipline(a.getActivityType())))
                .mapToLong(a -> a.getDuration() == null ? 0 : a.getDuration())
                .sum();
        return km > 0 ? seconds / km : 0;
    }

    // --- shared helpers --------------------------------------------------

    /** The token owner's activities in the contract's disciplines, newest first. */
    private List<Activity> mappableActivities(Long userId) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(a -> discipline(a.getActivityType()) != null)
                .sorted(Comparator.comparing(Activity::getStartTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Activity resolveViewableActivity(Long viewerId, String pipelineId) {
        Long activityId = parsePipelineActivityId(pipelineId);
        return activityRepository.findById(activityId)
                .filter(a -> activityVisibility.isViewableBy(viewerId, a))
                .filter(a -> discipline(a.getActivityType()) != null)
                .orElseThrow(() -> new ApiRequestException("Activity not found", HttpStatus.NOT_FOUND));
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

    private PipelineActivityDto toActivityDto(Activity activity) {
        boolean hasGps = GeoUtils.decodePolyline(activity.getRoutePolyline()).size() > 1;
        return new PipelineActivityDto(
                "act-" + activity.getId(),
                discipline(activity.getActivityType()),
                activity.getTitle(),
                toOffset(activity.getStartTime()),
                activity.getDuration() == null ? 0 : activity.getDuration(),
                activity.getDistance() == null ? 0 : activity.getDistance(),
                activity.getAverageHeartRate() == null ? 0 : activity.getAverageHeartRate(),
                activity.getMaxHeartRate() == null ? 0 : activity.getMaxHeartRate(),
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

    private double weekDistanceKm(List<Activity> activities, String discipline, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return activities.stream()
                .filter(a -> discipline.equals(discipline(a.getActivityType())))
                .filter(a -> inRange(a.getStartTime(), from, to))
                .mapToDouble(a -> a.getDistance() == null ? 0 : a.getDistance())
                .sum();
    }

    private int weekDurationSeconds(List<Activity> activities, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return activities.stream()
                .filter(a -> inRange(a.getStartTime(), from, to))
                .mapToInt(a -> a.getDuration() == null ? 0 : a.getDuration())
                .sum();
    }

    private int weekTss(List<Activity> activities, LocalDate weekStart) {
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
        return (int) Math.round(activities.stream()
                .filter(a -> inRange(a.getStartTime(), from, to))
                .mapToDouble(loadCalculator::tss)
                .sum());
    }

    private double disciplineKm(List<Activity> activities, String discipline) {
        return activities.stream()
                .filter(a -> discipline.equals(discipline(a.getActivityType())))
                .mapToDouble(a -> a.getDistance() == null ? 0 : a.getDistance())
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
