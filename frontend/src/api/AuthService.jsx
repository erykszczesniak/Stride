import apiClient from "./ApiClient";

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
