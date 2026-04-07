import apiClient from "./ApiClient";

class NotificationService {
  getNotifications() {
    return apiClient.get(`/notifications`);
  }

  getUnreadCount() {
    return apiClient.get(`/notifications/unread-count`);
  }

  markAsRead(notificationId) {
    return apiClient.put(`/notifications/${notificationId}/read`);
  }

  markAllAsRead() {
    return apiClient.put(`/notifications/read-all`);
  }
}

const notificationService = new NotificationService();
export default notificationService;
