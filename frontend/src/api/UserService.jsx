import apiClient from "./ApiClient";

class UserService {
  getUserById(userId) {
    return apiClient.get(`/users/${userId}`);
  }

  getCurrentUser() {
    return apiClient.get(`/users/me`);
  }

  updateProfile(profileData) {
    return apiClient.put(`/users/me`, profileData);
  }

  followUser(targetUserId) {
    return apiClient.post(`/users/${targetUserId}/follow`);
  }

  unfollowUser(targetUserId) {
    return apiClient.delete(`/users/${targetUserId}/follow`);
  }

  getFollowers(userId, page = 0, size = 20) {
    return apiClient.get(`/users/${userId}/followers?page=${page}&size=${size}`);
  }

  getFollowing(userId, page = 0, size = 20) {
    return apiClient.get(`/users/${userId}/following?page=${page}&size=${size}`);
  }

  getSuggestedUsers(limit = 5) {
    return apiClient.get(`/users/suggested?limit=${limit}`);
  }
}

export default new UserService();
