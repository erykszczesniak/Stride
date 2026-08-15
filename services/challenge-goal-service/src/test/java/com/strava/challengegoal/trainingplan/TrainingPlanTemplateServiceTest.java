package com.strava.challengegoal.trainingplan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrainingPlanTemplateService")
class TrainingPlanTemplateServiceTest {

    private TrainingPlanTemplateService service;

    @BeforeEach
    void setUp() {
        service = new TrainingPlanTemplateService();
    }

    @Nested
    @DisplayName("getAllTemplates")
    class GetAllTemplates {

        @Test
        @DisplayName("should return all three templates")
        void shouldReturnAllThreeTemplates() {
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();

            assertThat(templates).hasSize(3);
            assertThat(templates).extracting(TrainingPlanTemplateResponse::getId)
                    .containsExactly("5k-beginner", "10k-intermediate", "half-marathon-advanced");
        }

        @Test
        @DisplayName("should return 5K beginner plan with correct details")
        void shouldReturn5KBeginnerPlanWithCorrectDetails() {
            TrainingPlanTemplateResponse fiveK = service.getAllTemplates().get(0);

            assertThat(fiveK.getId()).isEqualTo("5k-beginner");
            assertThat(fiveK.getTitle()).isEqualTo("5K for Beginners");
            assertThat(fiveK.getLevel()).isEqualTo("Beginner");
            assertThat(fiveK.getDuration()).isEqualTo("8 weeks");
            assertThat(fiveK.getFrequency()).isEqualTo("3x per week");
            assertThat(fiveK.getGoal()).isEqualTo("5 km");
            assertThat(fiveK.getWeeks()).hasSize(8);
        }

        @Test
        @DisplayName("should return 10K intermediate plan with correct details")
        void shouldReturn10KIntermediatePlanWithCorrectDetails() {
            TrainingPlanTemplateResponse tenK = service.getAllTemplates().get(1);

            assertThat(tenK.getId()).isEqualTo("10k-intermediate");
            assertThat(tenK.getTitle()).isEqualTo("10K for Intermediate Runners");
            assertThat(tenK.getLevel()).isEqualTo("Intermediate");
            assertThat(tenK.getDuration()).isEqualTo("10 weeks");
            assertThat(tenK.getFrequency()).isEqualTo("4x per week");
            assertThat(tenK.getGoal()).isEqualTo("10 km");
            assertThat(tenK.getWeeks()).hasSize(10);
        }

        @Test
        @DisplayName("should return half marathon advanced plan with correct details")
        void shouldReturnHalfMarathonAdvancedPlanWithCorrectDetails() {
            TrainingPlanTemplateResponse halfMarathon = service.getAllTemplates().get(2);

            assertThat(halfMarathon.getId()).isEqualTo("half-marathon-advanced");
            assertThat(halfMarathon.getTitle()).isEqualTo("Half Marathon - Advanced Level");
            assertThat(halfMarathon.getLevel()).isEqualTo("Advanced");
            assertThat(halfMarathon.getDuration()).isEqualTo("12 weeks");
            assertThat(halfMarathon.getFrequency()).isEqualTo("5x per week");
            assertThat(halfMarathon.getGoal()).isEqualTo("21.1 km");
            assertThat(halfMarathon.getWeeks()).hasSize(12);
        }

        @Test
        @DisplayName("should have workouts in each week")
        void shouldHaveWorkoutsInEachWeek() {
            for (TrainingPlanTemplateResponse template : service.getAllTemplates()) {
                for (TrainingPlanTemplateResponse.WeekTemplate week : template.getWeeks()) {
                    assertThat(week.getWorkouts())
                            .as("Week %d of %s should have workouts", week.getWeek(), template.getId())
                            .isNotEmpty();
                }
            }
        }
    }

    @Nested
    @DisplayName("getTemplateById")
    class GetTemplateById {

        @Test
        @DisplayName("should return template when id exists")
        void shouldReturnTemplateWhenIdExists() {
            TrainingPlanTemplateResponse template = service.getTemplateById("5k-beginner");

            assertThat(template).isNotNull();
            assertThat(template.getId()).isEqualTo("5k-beginner");
            assertThat(template.getTitle()).isEqualTo("5K for Beginners");
        }

        @Test
        @DisplayName("should return null when template id does not exist")
        void shouldReturnNullWhenTemplateNotFound() {
            assertThat(service.getTemplateById("non-existent-plan")).isNull();
        }

        @Test
        @DisplayName("should return null when id is null")
        void shouldReturnNullWhenIdIsNull() {
            assertThat(service.getTemplateById(null)).isNull();
        }
    }
}
