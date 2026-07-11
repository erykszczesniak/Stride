package com.strava.backend.service.pipeline;

import com.strava.backend.domain.Activity;
import com.strava.backend.enums.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TrainingLoadCalculator")
class TrainingLoadCalculatorTest {

    private final TrainingLoadCalculator calculator = new TrainingLoadCalculator();

    private static Activity activity(ActivityType type, int durationSeconds, Integer avgHr) {
        Activity activity = Activity.builder()
                .title("test")
                .activityType(type)
                .distance(10.0)
                .duration(durationSeconds)
                .startTime(LocalDateTime.now())
                .build();
        activity.setAverageHeartRate(avgHr);
        return activity;
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
        Activity morning = activity(ActivityType.RUN, 3600, 170);
        Activity evening = activity(ActivityType.RUN, 3600, 170);
        LocalDateTime start = LocalDate.now().atTime(7, 0);
        morning.setStartTime(start);
        evening.setStartTime(start.plusHours(10));

        Map<LocalDate, Double> daily = calculator.dailyTss(List.of(morning, evening));

        assertThat(daily).hasSize(1);
        assertThat(daily.get(LocalDate.now())).isEqualTo(200.0, within(0.01));
    }
}
