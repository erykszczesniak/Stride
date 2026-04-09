package com.strava.backend.service;

import com.strava.backend.dto.training.TrainingPlanTemplateResponse;
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
        service = new com.strava.backend.service.impl.TrainingPlanTemplateServiceImpl();
    }

    @Nested
    @DisplayName("getAllTemplates")
    class GetAllTemplates {

        @Test
        @DisplayName("should return all three templates")
        void shouldReturnAllThreeTemplates() {
            // When
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();

            // Then
            assertThat(templates).hasSize(3);
            assertThat(templates).extracting(TrainingPlanTemplateResponse::getId)
                    .containsExactly("5k-beginner", "10k-intermediate", "half-marathon-advanced");
        }

        @Test
        @DisplayName("should return 5K beginner plan with correct details")
        void shouldReturn5KBeginnerPlanWithCorrectDetails() {
            // When
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();
            TrainingPlanTemplateResponse fiveK = templates.get(0);

            // Then
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
            // When
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();
            TrainingPlanTemplateResponse tenK = templates.get(1);

            // Then
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
            // When
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();
            TrainingPlanTemplateResponse halfMarathon = templates.get(2);

            // Then
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
            // When
            List<TrainingPlanTemplateResponse> templates = service.getAllTemplates();

            // Then
            for (TrainingPlanTemplateResponse template : templates) {
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
        @DisplayName("should return 5K beginner template when id is 5k-beginner")
        void shouldReturn5KBeginnerTemplate() {
            // When
            TrainingPlanTemplateResponse template = service.getTemplateById("5k-beginner");

            // Then
            assertThat(template).isNotNull();
            assertThat(template.getId()).isEqualTo("5k-beginner");
            assertThat(template.getTitle()).isEqualTo("5K for Beginners");
        }

        @Test
        @DisplayName("should return 10K intermediate template when id is 10k-intermediate")
        void shouldReturn10KIntermediateTemplate() {
            // When
            TrainingPlanTemplateResponse template = service.getTemplateById("10k-intermediate");

            // Then
            assertThat(template).isNotNull();
            assertThat(template.getId()).isEqualTo("10k-intermediate");
            assertThat(template.getTitle()).isEqualTo("10K for Intermediate Runners");
        }

        @Test
        @DisplayName("should return half marathon template when id is half-marathon-advanced")
        void shouldReturnHalfMarathonTemplate() {
            // When
            TrainingPlanTemplateResponse template = service.getTemplateById("half-marathon-advanced");

            // Then
            assertThat(template).isNotNull();
            assertThat(template.getId()).isEqualTo("half-marathon-advanced");
            assertThat(template.getTitle()).isEqualTo("Half Marathon - Advanced Level");
        }

        @Test
        @DisplayName("should return null when template id does not exist")
        void shouldReturnNullWhenTemplateNotFound() {
            // When
            TrainingPlanTemplateResponse template = service.getTemplateById("non-existent-plan");

            // Then
            assertThat(template).isNull();
        }

        @Test
        @DisplayName("should return null when id is null")
        void shouldReturnNullWhenIdIsNull() {
            // When
            TrainingPlanTemplateResponse template = service.getTemplateById(null);

            // Then
            assertThat(template).isNull();
        }
    }
}
