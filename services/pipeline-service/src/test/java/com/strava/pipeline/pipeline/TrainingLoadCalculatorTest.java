package com.strava.pipeline.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Ported from main — same model, same constants, ActivityData instead of the entity. */
@DisplayName("TrainingLoadCalculator")
class TrainingLoadCalculatorTest {

    private final TrainingLoadCalculator calculator = new TrainingLoadCalculator();

    private static ActivityData activity(ActivityType type, int durationSeconds, Integer avgHr) {
        return ActivityData.builder()
                .title("test")
                .activityType(type)
                .distance(10.0)
                .duration(durationSeconds)
                .averageHeartRate(avgHr)
                .startTime(LocalDateTime.now())
                .build();
    }

    private static ActivityData at(ActivityData activity, LocalDateTime startTime) {
        return ActivityData.builder()
                .title(activity.title())
                .activityType(activity.activityType())
                .distance(activity.distance())
                .duration(activity.duration())
                .averageHeartRate(activity.averageHeartRate())
                .startTime(startTime)
                .build();
    }

    @Test
    @DisplayName("one hour at threshold heart rate is exactly 100 TSS")
    void tssAtThreshold() {
        assertThat(calculator.tss(activity(ActivityType.RUN, 3600, 170))).isEqualTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("TSS scales with the square of the intensity factor")
    void tssScalesWithIntensitySquared() {
        // IF = 153/170 = 0.9 → 0.81 * 100
        assertThat(calculator.tss(activity(ActivityType.RUN, 3600, 153))).isEqualTo(81.0, within(0.01));
    }

    @Test
    @DisplayName("without heart rate a per-discipline intensity factor applies")
    void tssWithoutHeartRate() {
        assertThat(calculator.tss(activity(ActivityType.RUN, 3600, null)))
                .isEqualTo(0.78 * 0.78 * 100, within(0.01));
        assertThat(calculator.tss(activity(ActivityType.RIDE, 3600, null)))
                .isEqualTo(0.70 * 0.70 * 100, within(0.01));
    }

    @Test
    @DisplayName("CTL converges to the daily TSS under constant load and TSB stays near zero")
    void ctlConvergesUnderConstantLoad() {
        Map<LocalDate, Double> daily = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 250; i++) {
            daily.put(today.minusDays(i), 100.0);
        }

        List<TrainingLoadCalculator.DayLoad> series =
                calculator.loadSeries(daily, today.minusDays(7), today);
        TrainingLoadCalculator.DayLoad last = series.get(series.size() - 1);

        assertThat(last.ctl()).isBetween(90.0, 100.0);
        assertThat(last.tsb()).isBetween(-5.0, 5.0);
    }

    @Test
    @DisplayName("a taper (recent rest) drops ATL faster than CTL, so TSB turns positive")
    void taperTurnsTsbPositive() {
        Map<LocalDate, Double> daily = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i < 250; i++) { // trained daily until 6 days ago
            daily.put(today.minusDays(i), 100.0);
        }

        List<TrainingLoadCalculator.DayLoad> series =
                calculator.loadSeries(daily, today, today);
        TrainingLoadCalculator.DayLoad last = series.get(series.size() - 1);

        assertThat(last.tsb()).isGreaterThan(10.0);
        assertThat(last.atl()).isLessThan(last.ctl());
    }

    @Test
    @DisplayName("dailyTss groups multiple same-day activities")
    void dailyTssGroupsByDay() {
        LocalDateTime start = LocalDate.now().atTime(7, 0);
        ActivityData morning = at(activity(ActivityType.RUN, 3600, 170), start);
        ActivityData evening = at(activity(ActivityType.RUN, 3600, 170), start.plusHours(10));

        Map<LocalDate, Double> daily = calculator.dailyTss(List.of(morning, evening));

        assertThat(daily).hasSize(1);
        assertThat(daily.get(LocalDate.now())).isEqualTo(200.0, within(0.01));
    }
}
