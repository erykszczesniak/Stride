import apiClient from "./ApiClient";

class ActivityService {
  // Get activity by ID
  getActivityById(activityId, userId = 1) {
    return apiClient.get(`/activities/${activityId}?userId=${userId}`);
  }

  // Get feed activities
  getFeedActivities(userId = 1, page = 0, size = 10) {
    return apiClient.get(`/activities/feed?userId=${userId}&page=${page}&size=${size}`);
  }

  // Get user's activities
  getUserActivities(targetUserId, userId = 1, page = 0, size = 10) {
    return apiClient.get(`/activities/user/${targetUserId}?userId=${userId}&page=${page}&size=${size}`);
  }

  // Get current user's activities
  getMyActivities(userId = 1, page = 0, size = 10) {
    return apiClient.get(`/activities/my?userId=${userId}&page=${page}&size=${size}`);
  }

  // Get public activities
  getPublicActivities(page = 0, size = 10) {
    return apiClient.get(`/activities/public?page=${page}&size=${size}`);
  }

  // Create activity
  createActivity(userId = 1, activityData) {
    return apiClient.post(`/activities?userId=${userId}`, activityData);
  }

  // Update activity
  updateActivity(activityId, userId = 1, activityData) {
    return apiClient.put(`/activities/${activityId}?userId=${userId}`, activityData);
  }

  // Delete activity
  deleteActivity(activityId, userId = 1) {
    return apiClient.delete(`/activities/${activityId}?userId=${userId}`);
  }

  // Like activity
  likeActivity(activityId, userId = 1) {
    return apiClient.post(`/activities/${activityId}/like?userId=${userId}`);
  }

  // Unlike activity
  unlikeActivity(activityId, userId = 1) {
    return apiClient.delete(`/activities/${activityId}/like?userId=${userId}`);
  }

  // Get monthly stats
  getMonthlyStats(userId = 1) {
    return apiClient.get(`/activities/stats/monthly?userId=${userId}`);
  }

  // Get weekly stats
  getWeeklyStats(userId = 1) {
    return apiClient.get(`/activities/stats/weekly?userId=${userId}`);
  }
}

export default new ActivityService();
