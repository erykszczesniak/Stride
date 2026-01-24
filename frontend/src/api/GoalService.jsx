import apiClient from "./ApiClient";

class GoalService {
  // Get all goals for user
  getUserGoals(userId = 1) {
    return apiClient.get(`/goals?userId=${userId}`);
  }

  // Get active goals
  getActiveGoals(userId = 1) {
    return apiClient.get(`/goals/active?userId=${userId}`);
  }

  // Get goal by ID
  getGoalById(goalId, userId = 1) {
    return apiClient.get(`/goals/${goalId}?userId=${userId}`);
  }

  // Create goal
  createGoal(userId = 1, goalData) {
    return apiClient.post(`/goals?userId=${userId}`, goalData);
  }

  // Update goal
  updateGoal(goalId, userId = 1, goalData) {
    return apiClient.put(`/goals/${goalId}?userId=${userId}`, goalData);
  }

  // Delete goal
  deleteGoal(goalId, userId = 1) {
    return apiClient.delete(`/goals/${goalId}?userId=${userId}`);
  }
}

export default new GoalService();
