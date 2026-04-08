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

  async savePreferences(preferences) {
    // Save notification preferences to backend
    await apiClient.put(`/users/me`, {
      emailNotifications: preferences.emailNotifications,
      pushNotifications: preferences.pushNotifications,
      weeklySummary: preferences.weeklySummary,
    });

    // Save local-only preferences to localStorage
    localStorage.setItem("userPreferences", JSON.stringify(preferences));
    return { success: true };
  }
}

const settingsService = new SettingsService();
export default settingsService;
