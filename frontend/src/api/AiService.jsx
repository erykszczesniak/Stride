import apiClient from "./ApiClient";

class AiService {
  generateGoal(goalType, timeframe, description) {
    return apiClient.post("/ai/generate-goal", {
      goalType,
      timeframe,
      description
    });
  }

  generateTrainingPlan(activityType, experienceLevel, targetDistance, weeks) {
    return apiClient.post("/ai/generate-plan", {
      activityType,
      experienceLevel,
      targetDistance: parseInt(targetDistance) || 10,
      weeks: parseInt(weeks) || 4
    });
  }
}

export default new AiService();
