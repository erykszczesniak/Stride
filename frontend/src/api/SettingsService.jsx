import apiClient from "./ApiClient";

class SettingsService {
  getCurrentUser() {
    return apiClient.get(`/users/me`);
  }

  updateProfile(profileData) {
    return apiClient.put(`/users/me`, profileData);
  }

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
