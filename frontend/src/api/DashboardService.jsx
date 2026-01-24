import apiClient from "./ApiClient";

class DashboardService {
  // Get dashboard data (aggregated)
  getDashboardData(userId = 1) {
    return apiClient.get(`/dashboard?userId=${userId}`);
  }

  // Get dashboard stats summary
  getStatsSummary(userId = 1) {
    return apiClient.get(`/dashboard/stats?userId=${userId}`);
  }
}

export default new DashboardService();
