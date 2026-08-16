package com.strava.aiservice.goal

import com.strava.aiservice.config.SecurityConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Contract slice test — mirrors the monolith's AIControllerTest case by case. */
@WebMvcTest(
    controllers = [AiController::class],
    properties = [
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false"
    ]
)
@Import(SecurityConfig::class)
@DisplayName("AiController")
class AiControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var groqService: GroqService

    @Nested
    @DisplayName("POST /api/v1/ai/generate-goal")
    inner class GenerateGoal {

        @Test
        @DisplayName("should generate goal with valid request")
        fun shouldGenerateGoalWithValidRequest() {
            val expectedResponse = """
                {
                    "smartGoal": "Run 5K in under 25 minutes within 3 months",
                    "analysis": "This is an achievable goal for beginners",
                    "milestones": ["Complete 2K run", "Complete 3K run", "Complete 5K run"],
                    "tips": ["Start slow", "Be consistent", "Rest adequately"]
                }
            """.trimIndent()

            whenever(groqService.generateGoal(anyString(), anyString(), anyString()))
                .thenReturn(expectedResponse)

            mockMvc.perform(
                post("/api/v1/ai/generate-goal")
                    .header("X-User-Id", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"goalType": "distance", "timeframe": "3months", "description": "I want to run 5K"}""")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.smartGoal").exists())
                .andExpect(jsonPath("$.analysis").exists())
                .andExpect(jsonPath("$.milestones").isArray)
                .andExpect(jsonPath("$.tips").isArray)

            verify(groqService).generateGoal("distance", "3months", "I want to run 5K")
        }

        @Test
        @DisplayName("should use default values when request parameters are missing")
        fun shouldUseDefaultValuesWhenParametersMissing() {
            whenever(groqService.generateGoal(eq("distance"), eq("month"), eq("")))
                .thenReturn("""{"smartGoal": "Default goal", "analysis": "Default analysis", "milestones": [], "tips": []}""")

            mockMvc.perform(
                post("/api/v1/ai/generate-goal")
                    .header("X-User-Id", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect(status().isOk)

            verify(groqService).generateGoal("distance", "month", "")
        }

        @Test
        @DisplayName("should reject unauthenticated requests")
        fun shouldRejectUnauthenticated() {
            mockMvc.perform(
                post("/api/v1/ai/generate-goal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"goalType": "distance"}""")
            )
                .andExpect(status().isForbidden)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ai/generate-plan")
    inner class GenerateTrainingPlan {

        @Test
        @DisplayName("should generate training plan with valid request")
        fun shouldGenerateTrainingPlanWithValidRequest() {
            val expectedResponse = """
                {
                    "description": "4-week running plan for intermediate runners",
                    "weeks": [
                        {
                            "week": 1,
                            "totalDistance": 20,
                            "description": "Base building week",
                            "workouts": [
                                {"day": "Mon", "type": "easy run", "distance": 5, "duration": 30, "description": "Easy pace run"}
                            ]
                        }
                    ]
                }
            """.trimIndent()

            whenever(groqService.generateTrainingPlan(anyString(), anyString(), any(), any()))
                .thenReturn(expectedResponse)

            mockMvc.perform(
                post("/api/v1/ai/generate-plan")
                    .header("X-User-Id", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"activityType": "running", "experienceLevel": "intermediate", "targetDistance": 10, "weeks": 4}""")
            )
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.weeks").isArray)
                .andExpect(jsonPath("$.weeks[0].week").value(1))
                .andExpect(jsonPath("$.weeks[0].workouts").isArray)

            verify(groqService).generateTrainingPlan("running", "intermediate", 10, 4)
        }

        @Test
        @DisplayName("should use default values when request parameters are missing")
        fun shouldUseDefaultValuesWhenParametersMissing() {
            whenever(groqService.generateTrainingPlan(eq("running"), eq("intermediate"), eq(10), eq(4)))
                .thenReturn("""{"description": "Default plan", "weeks": []}""")

            mockMvc.perform(
                post("/api/v1/ai/generate-plan")
                    .header("X-User-Id", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect(status().isOk)

            verify(groqService).generateTrainingPlan("running", "intermediate", 10, 4)
        }

        @Test
        @DisplayName("should handle string values for numeric parameters")
        fun shouldHandleStringValuesForNumericParameters() {
            whenever(groqService.generateTrainingPlan(anyString(), anyString(), eq(15), eq(8)))
                .thenReturn("""{"description": "Custom plan", "weeks": []}""")

            mockMvc.perform(
                post("/api/v1/ai/generate-plan")
                    .header("X-User-Id", "1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"activityType": "cycling", "experienceLevel": "beginner", "targetDistance": "15", "weeks": "8"}""")
            )
                .andExpect(status().isOk)

            verify(groqService).generateTrainingPlan("cycling", "beginner", 15, 8)
        }

        @Test
        @DisplayName("should reject unauthenticated requests")
        fun shouldRejectUnauthenticated() {
            mockMvc.perform(
                post("/api/v1/ai/generate-plan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"activityType": "running"}""")
            )
                .andExpect(status().isForbidden)
        }
    }
}
