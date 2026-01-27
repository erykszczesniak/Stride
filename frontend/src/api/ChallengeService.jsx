import apiClient from "./ApiClient";

class ChallengeService {
  // Get active challenges (for Explore tab)
  getActiveChallenges(userId = 1, page = 0, size = 20) {
    return apiClient.get(`/challenges?userId=${userId}&page=${page}&size=${size}`);
  }

  // Get user's challenges (for My Challenges tab)
  getMyChallenges(userId = 1) {
    return apiClient.get(`/challenges/my?userId=${userId}`);
  }

  // Get challenge by ID
  getChallengeById(challengeId, userId = 1) {
    return apiClient.get(`/challenges/${challengeId}?userId=${userId}`);
  }

  // Join a challenge
  joinChallenge(challengeId, userId = 1) {
    return apiClient.post(`/challenges/${challengeId}/participate?userId=${userId}`);
  }

  // Leave a challenge
  leaveChallenge(challengeId, userId = 1) {
    return apiClient.delete(`/challenges/${challengeId}/participate?userId=${userId}`);
  }

  // Get challenge leaderboard
  getLeaderboard(challengeId) {
    return apiClient.get(`/challenges/${challengeId}/leaderboard`);
  }

  // Search challenges
  searchChallenges(query, userId = 1, page = 0, size = 20) {
    return apiClient.get(`/challenges/search?q=${encodeURIComponent(query)}&userId=${userId}&page=${page}&size=${size}`);
  }
}

export default new ChallengeService();
