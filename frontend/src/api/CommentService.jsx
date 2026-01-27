import apiClient from "./ApiClient";

class CommentService {
  getActivityComments(activityId) {
    return apiClient.get(`/activities/${activityId}/comments`);
  }

  createComment(activityId, content, userId = 1) {
    return apiClient.post(`/activities/${activityId}/comments?userId=${userId}`, { content });
  }

  updateComment(activityId, commentId, content, userId = 1) {
    return apiClient.put(`/activities/${activityId}/comments/${commentId}?userId=${userId}`, { content });
  }

  deleteComment(activityId, commentId, userId = 1) {
    return apiClient.delete(`/activities/${activityId}/comments/${commentId}?userId=${userId}`);
  }
}

export default new CommentService();
