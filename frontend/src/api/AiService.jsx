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

  // --- AI activity-analysis chat (Claude-powered) ---

  getActivityChat(activityId) {
    return apiClient.get(`/ai/activities/${activityId}/chat`);
  }

  sendActivityChatMessage(activityId, message) {
    return apiClient.post(`/ai/activities/${activityId}/chat`, { message });
  }

  clearActivityChat(activityId) {
    return apiClient.delete(`/ai/activities/${activityId}/chat`);
  }
}

const aiService = new AiService();
export default aiService;
