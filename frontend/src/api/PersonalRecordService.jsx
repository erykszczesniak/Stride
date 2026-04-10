import apiClient from "./ApiClient";

class PersonalRecordService {
  getMyPersonalRecords() {
    return apiClient.get(`/personal-records`);
  }

  getUserPersonalRecords(userId) {
    return apiClient.get(`/personal-records/user/${userId}`);
  }

  getPersonalRecordsByActivityType(activityType) {
    return apiClient.get(`/personal-records/activity-type/${activityType}`);
  }

  recalculateAllPRs() {
    return apiClient.post(`/personal-records/recalculate`);
  }
}

const personalRecordService = new PersonalRecordService();
export default personalRecordService;
