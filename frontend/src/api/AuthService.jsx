import apiClient from "./ApiClient";

// Base URL the apiClient uses for its requests. We reconstruct it here so the
// "Continue with Strava" button can point to the backend /authorize endpoint
// as an absolute URL (window.location assignment, not an ajax call).
const API_BASE_URL = process.env.REACT_APP_API_URL || "/api/v1";

class AuthService {
  login(email, password) {
    return apiClient.post("/auth/login", { email, password });
  }

  register(firstName, lastName, email, password, confirmPassword) {
    return apiClient.post("/auth/register", {
      firstName,
      lastName,
      email,
      password,
      confirmPassword,
    });
  }

  googleLogin(idToken) {
    return apiClient.post("/auth/google", { idToken });
  }

  /**
   * Absolute URL for the backend's Strava OAuth2 /authorize endpoint. The backend
   * issues a 302 to strava.com from there, so callers do `window.location = url`
   * rather than fetching it.
   */
  stravaAuthorizeUrl() {
    return `${API_BASE_URL}/auth/strava/authorize?redirect=true`;
  }

  refreshToken(refreshToken) {
    return apiClient.post("/auth/refresh", { refreshToken });
  }

  forgotPassword(email) {
    return apiClient.post("/auth/forgot-password", { email });
  }

  resetPassword(code, password) {
    return apiClient.post("/auth/reset-password", { code, password });
  }

  getCurrentUser() {
    return apiClient.get("/users/me");
  }
}

const authService = new AuthService();
export default authService;
