import apiClient from "./ApiClient";

class SettingsService {
  getCurrentUser(userId = 1) {
    return apiClient.get(`/users/me?userId=${userId}`);
  }

  updateProfile(userId = 1, profileData) {
    return apiClient.put(`/users/me?userId=${userId}`, profileData);
  }

  // Local storage methods for preferences
  getPreferences() {
    const defaults = {
      emailNotifications: true,
      pushNotifications: true,
      weeklySummary: true,
      units: "metric",
      defaultActivityType: "RUN",
      paceDisplay: "pace",
      language: "en",
      darkMode: false,
    };

    const stored = localStorage.getItem("userPreferences");
    if (stored) {
      return { ...defaults, ...JSON.parse(stored) };
    }
    return defaults;
  }

  savePreferences(preferences) {
    localStorage.setItem("userPreferences", JSON.stringify(preferences));
    return Promise.resolve({ success: true });
  }
}

export default new SettingsService();
