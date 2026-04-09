package com.strava.backend.service.impl;

import com.strava.backend.dto.training.TrainingPlanTemplateResponse;
import com.strava.backend.dto.training.TrainingPlanTemplateResponse.WeekTemplate;
import com.strava.backend.dto.training.TrainingPlanTemplateResponse.WorkoutTemplate;
import com.strava.backend.service.TrainingPlanTemplateService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class TrainingPlanTemplateServiceImpl implements TrainingPlanTemplateService {

    @Override
    public List<TrainingPlanTemplateResponse> getAllTemplates() {
        return Arrays.asList(
                create5KBeginnerPlan(),
                create10KIntermediatePlan(),
                createHalfMarathonAdvancedPlan()
        );
    }

    @Override
    public TrainingPlanTemplateResponse getTemplateById(String id) {
        return getAllTemplates().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private TrainingPlanTemplateResponse create5KBeginnerPlan() {
        return TrainingPlanTemplateResponse.builder()
                .id("5k-beginner")
                .title("5K for Beginners")
                .description("Perfect plan for those starting their running journey")
                .level("Beginner")
                .levelColor("#22c55e")
                .duration("8 weeks")
                .frequency("3x per week")
                .goal("5 km")
                .gradient("linear-gradient(135deg, #22c55e 0%, #16a34a 100%)")
                .icon("\u26A1")
                .weeks(Arrays.asList(
                        WeekTemplate.builder().week(1).name("Week 1").description("Basics and building the habit").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run/Walk").description("5 min warm-up, 10x (1 min run + 1 min walk), 5 min cool-down").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Run/Walk").description("5 min warm-up, 10x (1 min run + 1 min walk), 5 min cool-down").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Run/Walk").description("5 min warm-up, 10x (1 min run + 1 min walk), 5 min cool-down").duration("20 min").build()
                        )).build(),
                        WeekTemplate.builder().week(2).name("Week 2").description("Building endurance").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run/Walk").description("5 min warm-up, 8x (90 sec run + 90 sec walk), 5 min cool-down").duration("22 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Run/Walk").description("5 min warm-up, 8x (90 sec run + 90 sec walk), 5 min cool-down").duration("22 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Run").description("5 min warm-up, 6x (2 min run + 1 min walk), 5 min cool-down").duration("25 min").build()
                        )).build(),
                        WeekTemplate.builder().week(3).name("Week 3").description("Building pace").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run").description("5 min warm-up, 5x (3 min run + 2 min walk), 5 min cool-down").duration("25 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Run").description("5 min warm-up, 5x (3 min run + 2 min walk), 5 min cool-down").duration("25 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Run").description("5 min warm-up, 4x (5 min run + 2 min walk), 3 min cool-down").duration("28 min").build()
                        )).build(),
                        WeekTemplate.builder().week(4).name("Week 4").description("Recovery").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Slow pace, recovery run").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Walk").description("Active recovery").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Run").description("5 min warm-up, 15 min continuous run, 5 min cool-down").duration("25 min").build()
                        )).build(),
                        WeekTemplate.builder().week(5).name("Week 5").description("Increasing distance").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run").description("5 min warm-up, 20 min continuous run, 5 min cool-down").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Run").description("5 min warm-up, 20 min continuous run, 5 min cool-down").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Long run").description("25 min continuous run at comfortable pace").duration("30 min").build()
                        )).build(),
                        WeekTemplate.builder().week(6).name("Week 6").description("Building confidence").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run").description("5 min warm-up, 25 min continuous run, 5 min cool-down").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("5 min warm-up, 6x (2 min fast + 2 min slow), 5 min cool-down").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Long run").description("30 min continuous run at easy pace").duration("35 min").build()
                        )).build(),
                        WeekTemplate.builder().week(7).name("Week 7").description("Peak training").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Run").description("5 min warm-up, 28 min continuous run, 5 min cool-down").duration("38 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Tempo run").description("5 min warm-up, 15 min tempo pace, 5 min cool-down").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Long run").description("35 min continuous run - your longest run!").duration("40 min").build()
                        )).build(),
                        WeekTemplate.builder().week(8).name("Week 8").description("Taper and race").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light 20 min jog").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Rest").description("Complete rest or light walk").duration("0 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("5K Race").description("Race day! Run your first 5K!").duration("25-35 min").build()
                        )).build()
                ))
                .build();
    }

    private TrainingPlanTemplateResponse create10KIntermediatePlan() {
        return TrainingPlanTemplateResponse.builder()
                .id("10k-intermediate")
                .title("10K for Intermediate Runners")
                .description("For experienced runners aiming to conquer 10K")
                .level("Intermediate")
                .levelColor("#3b82f6")
                .duration("10 weeks")
                .frequency("4x per week")
                .goal("10 km")
                .gradient("linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)")
                .icon("\uD83D\uDCC8")
                .weeks(Arrays.asList(
                        WeekTemplate.builder().week(1).name("Week 1").description("Building the base").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("10 min warm-up, 20 min race pace, 5 min cool-down").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("10 min warm-up, 6x (3 min fast + 2 min slow), 5 min cool-down").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Slow recovery pace").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Steady pace, building endurance").duration("60 min").build()
                        )).build(),
                        WeekTemplate.builder().week(2).name("Week 2").description("Working on pace").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("10 min warm-up, 25 min threshold pace, 5 min cool-down").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("10 min warm-up, 8x (2 min fast + 90 sec slow), 5 min cool-down").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery pace").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Comfortable long-distance pace").duration("70 min").build()
                        )).build(),
                        WeekTemplate.builder().week(3).name("Week 3").description("Increasing intensity").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("10 min warm-up, 30 min tempo, 5 min cool-down").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Hill repeats").description("10 min warm-up, 8x hill sprints, 10 min cool-down").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery pace").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Progressive long run").duration("75 min").build()
                        )).build(),
                        WeekTemplate.builder().week(4).name("Week 4").description("Recovery week").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light jog").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Tempo run").description("Moderate tempo").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery pace").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Reduced distance").duration("50 min").build()
                        )).build(),
                        WeekTemplate.builder().week(5).name("Week 5").description("Building back up").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("Strong tempo effort").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("10x 400m at 5K pace with 90 sec rest").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Building back endurance").duration("80 min").build()
                        )).build(),
                        WeekTemplate.builder().week(6).name("Week 6").description("Peak training begins").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("Extended tempo").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("6x 800m at goal pace with 2 min rest").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Longest run of the plan").duration("90 min").build()
                        )).build(),
                        WeekTemplate.builder().week(7).name("Week 7").description("Maintaining fitness").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("Quality tempo").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("Race-pace intervals").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Steady state").duration("80 min").build()
                        )).build(),
                        WeekTemplate.builder().week(8).name("Week 8").description("Sharpening").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("Race simulation").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Intervals").description("5x 1000m at goal pace").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Light recovery").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Moderate distance").duration("70 min").build()
                        )).build(),
                        WeekTemplate.builder().week(9).name("Week 9").description("Taper begins").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Tempo run").description("Short and sharp").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Easy run").description("Light jog with strides").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Easy run").description("Recovery").duration("25 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Final long run - reduced").duration("50 min").build()
                        )).build(),
                        WeekTemplate.builder().week(10).name("Week 10").description("Race week").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light shake-out").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Easy run").description("Short jog with strides").duration("15 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Rest").description("Complete rest").duration("0 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("10K Race").description("Race day! Achieve your 10K goal!").duration("45-60 min").build()
                        )).build()
                ))
                .build();
    }

    private TrainingPlanTemplateResponse createHalfMarathonAdvancedPlan() {
        return TrainingPlanTemplateResponse.builder()
                .id("half-marathon-advanced")
                .title("Half Marathon - Advanced Level")
                .description("Intensive plan for experienced runners targeting a half marathon")
                .level("Advanced")
                .levelColor("#a855f7")
                .duration("12 weeks")
                .frequency("5x per week")
                .goal("21.1 km")
                .gradient("linear-gradient(135deg, #a855f7 0%, #7c3aed 100%)")
                .icon("\uD83C\uDFC6")
                .weeks(Arrays.asList(
                        WeekTemplate.builder().week(1).name("Week 1").description("Volume acclimatization").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Base building").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("15 min warm-up, 25 min tempo, 10 min cool-down").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("8x 600m at 5K pace").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Recovery").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Progressive pace").duration("90 min").build()
                        )).build(),
                        WeekTemplate.builder().week(2).name("Week 2").description("Increasing intensity").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("15 min warm-up, 30 min tempo, 10 min cool-down").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("10x 400m at mile pace").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Light jog").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Steady state").duration("100 min").build()
                        )).build(),
                        WeekTemplate.builder().week(3).name("Week 3").description("Building strength").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Hill repeats").description("10x hill sprints").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Tempo run").description("Extended threshold").duration("60 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Recovery").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Distance focus").duration("110 min").build()
                        )).build(),
                        WeekTemplate.builder().week(4).name("Week 4").description("Recovery week").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light jog").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Moderate tempo").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Easy run").description("Recovery").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Rest").description("Complete rest").duration("0 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Reduced distance").duration("70 min").build()
                        )).build(),
                        WeekTemplate.builder().week(5).name("Week 5").description("Building back up").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Base building").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Race pace work").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("6x 1000m at goal pace").duration("60 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Progressive").duration("110 min").build()
                        )).build(),
                        WeekTemplate.builder().week(6).name("Week 6").description("Peak volume").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Extended tempo").duration("60 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("5x 1600m at goal pace").duration("65 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Light").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Longest run - 18-20km").duration("120 min").build()
                        )).build(),
                        WeekTemplate.builder().week(7).name("Week 7").description("Maintaining peak").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Quality tempo").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("Race simulation intervals").duration("60 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Light jog").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Steady state").duration("110 min").build()
                        )).build(),
                        WeekTemplate.builder().week(8).name("Week 8").description("Recovery week").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Short tempo").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Easy run").description("Recovery").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Rest").description("Complete rest").duration("0 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Moderate").duration("80 min").build()
                        )).build(),
                        WeekTemplate.builder().week(9).name("Week 9").description("Final build").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("50 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Race pace").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("8x 800m at goal pace").duration("55 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Light").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Final long run - 16km").duration("100 min").build()
                        )).build(),
                        WeekTemplate.builder().week(10).name("Week 10").description("Taper begins").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Recovery").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Short and sharp").duration("45 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Intervals").description("6x 400m at mile pace").duration("40 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Light").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Reduced - 12km").duration("75 min").build()
                        )).build(),
                        WeekTemplate.builder().week(11).name("Week 11").description("Deep taper").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Light jog").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Tuesday").type("Tempo run").description("Short tempo").duration("35 min").build(),
                                WorkoutTemplate.builder().day("Thursday").type("Easy run").description("Strides included").duration("30 min").build(),
                                WorkoutTemplate.builder().day("Saturday").type("Easy run").description("Shake-out").duration("25 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Long run").description("Short long run - 8km").duration("50 min").build()
                        )).build(),
                        WeekTemplate.builder().week(12).name("Week 12").description("Race week").workouts(Arrays.asList(
                                WorkoutTemplate.builder().day("Monday").type("Easy run").description("Very light").duration("25 min").build(),
                                WorkoutTemplate.builder().day("Wednesday").type("Easy run").description("Strides").duration("20 min").build(),
                                WorkoutTemplate.builder().day("Friday").type("Rest").description("Complete rest").duration("0 min").build(),
                                WorkoutTemplate.builder().day("Sunday").type("Half Marathon").description("Race day! Run your half marathon!").duration("90-120 min").build()
                        )).build()
                ))
                .build();
    }
}
