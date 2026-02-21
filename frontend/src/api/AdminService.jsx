import apiClient from "./ApiClient";

class AdminService {
  getAdminStats() {
    return apiClient.get("/admin/stats");
  }

  getAllUsers(page = 0, size = 20) {
    return apiClient.get(`/admin/users?page=${page}&size=${size}`);
  }

  getAllActivities(page = 0, size = 20) {
    return apiClient.get(`/admin/activities?page=${page}&size=${size}`);
  }

  searchActivities(search, page = 0, size = 20) {
    return apiClient.get(`/admin/activities?search=${encodeURIComponent(search)}&page=${page}&size=${size}`);
  }

  deleteActivity(activityId) {
    return apiClient.delete(`/admin/activities/${activityId}`);
  }

  updateUserRole(userId, role) {
    return apiClient.put(`/admin/users/${userId}/role`, { role });
  }

  deleteUser(userId) {
    return apiClient.delete(`/admin/users/${userId}`);
  }
}

const adminService = new AdminService();
export default adminService;
