import apiClient from "./ApiClient";

class GearService {
  getMyGear(includeRetired = false) {
    const params = includeRetired ? "?includeRetired=true" : "";
    return apiClient.get(`/gear${params}`);
  }

  getGearById(gearId) {
    return apiClient.get(`/gear/${gearId}`);
  }

  createGear(data) {
    return apiClient.post(`/gear`, data);
  }

  updateGear(gearId, data) {
    return apiClient.put(`/gear/${gearId}`, data);
  }

  retireGear(gearId) {
    return apiClient.post(`/gear/${gearId}/retire`);
  }

  deleteGear(gearId) {
    return apiClient.delete(`/gear/${gearId}`);
  }
}

const gearService = new GearService();
export default gearService;
