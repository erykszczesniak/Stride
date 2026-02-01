import apiClient from "./ApiClient";

class CommentService {
  getActivityComments(activityId) {
    return apiClient.get(`/activities/${activityId}/comments`);
  }

  createComment(activityId, content) {
    return apiClient.post(`/activities/${activityId}/comments`, { content });
  }

  updateComment(activityId, commentId, content) {
    return apiClient.put(`/activities/${activityId}/comments/${commentId}`, { content });
  }

  deleteComment(activityId, commentId) {
    return apiClient.delete(`/activities/${activityId}/comments/${commentId}`);
  }
}

export default new CommentService();
