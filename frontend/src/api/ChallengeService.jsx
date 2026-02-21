import apiClient from "./ApiClient";

class ChallengeService {
  getActiveChallenges(page = 0, size = 20) {
    return apiClient.get(`/challenges?page=${page}&size=${size}`);
  }

  getMyChallenges() {
    return apiClient.get(`/challenges/my`);
  }

  getChallengeById(challengeId) {
    return apiClient.get(`/challenges/${challengeId}`);
  }

  joinChallenge(challengeId) {
    return apiClient.post(`/challenges/${challengeId}/participate`);
  }

  leaveChallenge(challengeId) {
    return apiClient.delete(`/challenges/${challengeId}/participate`);
  }

  getLeaderboard(challengeId) {
    return apiClient.get(`/challenges/${challengeId}/leaderboard`);
  }

  searchChallenges(query, page = 0, size = 20) {
    return apiClient.get(`/challenges/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
  }
}

const challengeService = new ChallengeService();
export default challengeService;
