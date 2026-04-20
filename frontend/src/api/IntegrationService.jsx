import apiClient from "./ApiClient";

/**
 * Client for the Strava OAuth2 + import integration endpoints.
 * Routes live under /api/v1/integrations/strava on the backend.
 */
class IntegrationService {
  // Fetches the pre-signed Strava OAuth URL. Caller does the window.location jump
  // because we can't attach the JWT to a raw browser navigation.
  getStravaAuthorizeUrl() {
    return apiClient.get(`/integrations/strava/authorize`);
  }

  getStravaStatus() {
    return apiClient.get(`/integrations/strava/status`);
  }

  importFromStrava() {
    return apiClient.post(`/integrations/strava/import`);
  }

  disconnectStrava() {
    return apiClient.delete(`/integrations/strava/disconnect`);
  }
}

const integrationService = new IntegrationService();
export default integrationService;
