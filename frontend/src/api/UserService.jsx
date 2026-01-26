import apiClient from "./ApiClient";

class UserService {
  // Get user by ID
  getUserById(userId) {
    return apiClient.get(`/users/${userId}`);
  }

  // Get user profile
  getUserProfile(userId, currentUserId = 1) {
    return apiClient.get(`/users/${userId}/profile?currentUserId=${currentUserId}`);
  }

  // Update user profile
  updateProfile(userId, profileData) {
    return apiClient.put(`/users/${userId}`, profileData);
  }

  // Follow user
  followUser(userId, targetUserId) {
    return apiClient.post(`/users/${userId}/follow/${targetUserId}`);
  }

  // Unfollow user
  unfollowUser(userId, targetUserId) {
    return apiClient.delete(`/users/${userId}/follow/${targetUserId}`);
  }

  // Get followers
  getFollowers(userId, page = 0, size = 20) {
    return apiClient.get(`/users/${userId}/followers?page=${page}&size=${size}`);
  }

  // Get following
  getFollowing(userId, page = 0, size = 20) {
    return apiClient.get(`/users/${userId}/following?page=${page}&size=${size}`);
  }

  // Get suggested users to follow
  getSuggestedUsers(userId, limit = 5) {
    return apiClient.get(`/users/suggested?userId=${userId}&limit=${limit}`);
  }
}

export default new UserService();
