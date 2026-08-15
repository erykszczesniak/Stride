package com.strava.challengegoal.trainingplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrainingPlanTemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TrainingPlanTemplateController")
class TrainingPlanTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainingPlanTemplateService templateService;

    @Nested
    @DisplayName("GET /api/v1/training-plans/templates")
    class GetAllTemplates {

        @Test
        @DisplayName("should return all templates")
        void shouldReturnAllTemplates() throws Exception {
            List<TrainingPlanTemplateResponse> templates = Arrays.asList(
                    TrainingPlanTemplateResponse.builder()
                            .id("5k-beginner")
                            .title("5K for Beginners")
                            .description("Perfect plan for those starting their running journey")
                            .level("Beginner")
                            .levelColor("#22c55e")
                            .duration("8 weeks")
                            .frequency("3x per week")
                            .goal("5 km")
                            .gradient("linear-gradient(135deg, #22c55e 0%, #16a34a 100%)")
                            .icon("⚡")
                            .weeks(Collections.emptyList())
                            .build(),
                    TrainingPlanTemplateResponse.builder()
                            .id("10k-intermediate")
                            .title("10K for Intermediate Runners")
                            .description("For experienced runners aiming to conquer 10K")
                            .level("Intermediate")
                            .levelColor("#3b82f6")
                            .duration("10 weeks")
                            .frequency("4x per week")
                            .goal("10 km")
                            .gradient("linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)")
                            .icon("📈")
                            .weeks(Collections.emptyList())
                            .build()
            );

            when(templateService.getAllTemplates()).thenReturn(templates);

            mockMvc.perform(get("/api/v1/training-plans/templates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value("5k-beginner"))
                    .andExpect(jsonPath("$[0].title").value("5K for Beginners"))
                    .andExpect(jsonPath("$[0].level").value("Beginner"))
                    .andExpect(jsonPath("$[1].id").value("10k-intermediate"))
                    .andExpect(jsonPath("$[1].title").value("10K for Intermediate Runners"));
        }

        @Test
        @DisplayName("should return empty list when no templates exist")
        void shouldReturnEmptyListWhenNoTemplates() throws Exception {
            when(templateService.getAllTemplates()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/training-plans/templates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/training-plans/templates/{id}")
    class GetTemplateById {

        @Test
        @DisplayName("should return template when found")
        void shouldReturnTemplateWhenFound() throws Exception {
            TrainingPlanTemplateResponse template = TrainingPlanTemplateResponse.builder()
                    .id("5k-beginner")
                    .title("5K for Beginners")
                    .level("Beginner")
                    .weeks(List.of(
                            TrainingPlanTemplateResponse.WeekTemplate.builder()
                                    .week(1)
                                    .name("Week 1")
                                    .description("Basics and building the habit")
                                    .workouts(List.of(
                                            TrainingPlanTemplateResponse.WorkoutTemplate.builder()
                                                    .day("Monday")
                                                    .type("Run/Walk")
                                                    .description("5 min warm-up, 10x intervals, 5 min cool-down")
                                                    .duration("20 min")
                                                    .build()
                                    ))
                                    .build()
                    ))
                    .build();

            when(templateService.getTemplateById("5k-beginner")).thenReturn(template);

            mockMvc.perform(get("/api/v1/training-plans/templates/5k-beginner"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("5k-beginner"))
                    .andExpect(jsonPath("$.title").value("5K for Beginners"))
                    .andExpect(jsonPath("$.level").value("Beginner"))
                    .andExpect(jsonPath("$.weeks").isArray())
                    .andExpect(jsonPath("$.weeks[0].week").value(1))
                    .andExpect(jsonPath("$.weeks[0].workouts[0].day").value("Monday"));
        }

        @Test
        @DisplayName("should return 404 when template not found")
        void shouldReturn404WhenTemplateNotFound() throws Exception {
            when(templateService.getTemplateById("non-existent")).thenReturn(null);

            mockMvc.perform(get("/api/v1/training-plans/templates/non-existent"))
                    .andExpect(status().isNotFound());
        }
    }
}
