import apiClient from "./ApiClient";

class GoalService {
  getUserGoals() {
    return apiClient.get(`/goals`);
  }

  getAllGoals() {
    return apiClient.get(`/goals/all`);
  }

  getGoalById(goalId) {
    return apiClient.get(`/goals/${goalId}`);
  }

  createGoal(goalData) {
    return apiClient.post(`/goals`, goalData);
  }

  updateGoal(goalId, goalData) {
    return apiClient.put(`/goals/${goalId}`, goalData);
  }

  deleteGoal(goalId) {
    return apiClient.delete(`/goals/${goalId}`);
  }
}

const goalService = new GoalService();
export default goalService;
