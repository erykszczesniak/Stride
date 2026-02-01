import apiClient from "./ApiClient";

class ClubService {
  getAllClubs(sportType = null, page = 0, size = 20) {
    let url = `/clubs?page=${page}&size=${size}`;
    if (sportType) {
      url += `&sportType=${sportType}`;
    }
    return apiClient.get(url);
  }

  getMyClubs() {
    return apiClient.get(`/clubs/my`);
  }

  getClubStats() {
    return apiClient.get(`/clubs/stats`);
  }

  getClubById(clubId) {
    return apiClient.get(`/clubs/${clubId}`);
  }

  joinClub(clubId) {
    return apiClient.post(`/clubs/${clubId}/join`);
  }

  leaveClub(clubId) {
    return apiClient.post(`/clubs/${clubId}/leave`);
  }

  getClubMembers(clubId) {
    return apiClient.get(`/clubs/${clubId}/members`);
  }

  searchClubs(query, page = 0, size = 20) {
    return apiClient.get(`/clubs/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
  }
}

export default new ClubService();
