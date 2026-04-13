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
    // Save local-only preferences to localStorage
    // (notification prefs are saved via updateProfile to avoid duplicate PUT)
    localStorage.setItem("userPreferences", JSON.stringify(preferences));
    return { success: true };
  }

  changePassword(currentPassword, newPassword) {
    return apiClient.put(`/users/me/password`, { currentPassword, newPassword });
  }

  deleteAccount() {
    return apiClient.delete(`/users/me`);
  }
}

const settingsService = new SettingsService();
export default settingsService;
