import apiClient from "./ApiClient";

class ClubService {
  // Get all clubs (with optional sportType filter)
  getAllClubs(userId = 1, sportType = null, page = 0, size = 20) {
    let url = `/clubs?userId=${userId}&page=${page}&size=${size}`;
    if (sportType) {
      url += `&sportType=${sportType}`;
    }
    return apiClient.get(url);
  }

  // Get user's clubs
  getMyClubs(userId = 1) {
    return apiClient.get(`/clubs/my?userId=${userId}`);
  }

  // Get club stats
  getClubStats(userId = 1) {
    return apiClient.get(`/clubs/stats?userId=${userId}`);
  }

  // Get club by ID
  getClubById(clubId, userId = 1) {
    return apiClient.get(`/clubs/${clubId}?userId=${userId}`);
  }

  // Join a club
  joinClub(clubId, userId = 1) {
    return apiClient.post(`/clubs/${clubId}/join?userId=${userId}`);
  }

  // Leave a club
  leaveClub(clubId, userId = 1) {
    return apiClient.post(`/clubs/${clubId}/leave?userId=${userId}`);
  }

  // Get club members
  getClubMembers(clubId) {
    return apiClient.get(`/clubs/${clubId}/members`);
  }

  // Search clubs
  searchClubs(query, userId = 1, page = 0, size = 20) {
    return apiClient.get(`/clubs/search?q=${encodeURIComponent(query)}&userId=${userId}&page=${page}&size=${size}`);
  }
}

export default new ClubService();
