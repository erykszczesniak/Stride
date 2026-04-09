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

  deleteNotification(notificationId) {
    return apiClient.delete(`/notifications/${notificationId}`);
  }

  clearAll() {
    return apiClient.delete(`/notifications`);
  }
}

const notificationService = new NotificationService();
export default notificationService;
