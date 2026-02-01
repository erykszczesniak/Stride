import apiClient from "./ApiClient";

class DashboardService {
  getDashboardData() {
    return apiClient.get(`/dashboard`);
  }

  getStatsSummary() {
    return apiClient.get(`/dashboard/stats`);
  }
}

export default new DashboardService();
