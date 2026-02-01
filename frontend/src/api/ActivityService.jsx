import apiClient from "./ApiClient";

class ActivityService {
  getActivityById(activityId) {
    return apiClient.get(`/activities/${activityId}`);
  }

  getFeedActivities(page = 0, size = 10) {
    return apiClient.get(`/activities/feed?page=${page}&size=${size}`);
  }

  getUserActivities(targetUserId, page = 0, size = 10) {
    return apiClient.get(`/activities/user/${targetUserId}?page=${page}&size=${size}`);
  }

  getMyActivities(page = 0, size = 10, search = '', activityType = null) {
    let url = `/activities/my?page=${page}&size=${size}`;
    if (search) {
      url += `&search=${encodeURIComponent(search)}`;
    }
    if (activityType) {
      url += `&activityType=${activityType}`;
    }
    return apiClient.get(url);
  }

  getMyTotalStats() {
    return apiClient.get(`/activities/my/stats`);
  }

  getPublicActivities(page = 0, size = 10) {
    return apiClient.get(`/activities/public?page=${page}&size=${size}`);
  }

  createActivity(activityData) {
    return apiClient.post(`/activities`, activityData);
  }

  updateActivity(activityId, activityData) {
    return apiClient.put(`/activities/${activityId}`, activityData);
  }

  deleteActivity(activityId) {
    return apiClient.delete(`/activities/${activityId}`);
  }

  likeActivity(activityId) {
    return apiClient.post(`/activities/${activityId}/like`);
  }

  unlikeActivity(activityId) {
    return apiClient.delete(`/activities/${activityId}/like`);
  }

  getMonthlyStats() {
    return apiClient.get(`/activities/stats/monthly`);
  }

  getWeeklyStats() {
    return apiClient.get(`/activities/stats/weekly`);
  }
}

export default new ActivityService();
