import React, { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiUser, FiBell, FiSliders, FiLock, FiChevronRight, FiSave, FiLogOut, FiSun, FiTrash2, FiLink, FiDownloadCloud, FiZap } from "react-icons/fi";
import Navbar from "../components/Navbar/Navbar";
import SettingsService from "../api/SettingsService";
import IntegrationService from "../api/IntegrationService";
import { useToast } from "../context/ToastContext";
import { useTheme } from "../context/ThemeContext";
import { useUnits } from "../context/UnitsContext";
import i18n, { SUPPORTED_LANGUAGES } from "../i18n";
import {
  SettingsContainer,
  SettingsContent,
  SettingsHeader,
  SettingsTitle,
  SaveButton,
  SettingsLayout,
  SettingsSidebar,
  SidebarItem,
  SidebarArrow,
  SettingsPanel,
  PanelTitle,
  PanelDescription,
  ProfileHeader,
  ProfileAvatar,
  ProfileInfo,
  ProfileName,
  ProfileEmail,
  FormGroup,
  FormLabel,
  FormInput,
  FormRow,
  FormSelect,
  ToggleItem,
  ToggleInfo,
  ToggleTitle,
  ToggleDescription,
  Toggle,
  DarkModeToggle,
  DarkModeIcon,
  AccountEmail,
  AccountEmailLabel,
  AccountEmailValue,
  SignOutButton,
  LoadingSpinner,
  SectionDivider,
  SectionTitle,
  DangerZone,
  DangerZoneTitle,
  DangerZoneDescription,
  DangerButton,
  ModalOverlay,
  ModalContent,
  ModalTitle,
  ModalText,
  ModalActions,
  ModalCancelButton,
  PasswordFormButton,
  FormError,
  IntegrationRow,
  IntegrationInfo,
  IntegrationName,
  IntegrationDetail,
  IntegrationActions,
  StravaConnectButton,
  SecondaryButton,
} from "./SettingsStyles";

const VALID_SECTIONS = ["profile", "notifications", "preferences", "account"];

const KG_TO_LBS = 2.20462;
const CM_TO_IN = 0.393701;

const formatDisplayWeight = (weightKg, isImperial) => {
  if (weightKg == null || weightKg === "") return "";
  const value = isImperial ? Number(weightKg) * KG_TO_LBS : Number(weightKg);
  return isImperial ? value.toFixed(1) : String(value);
};

const formatDisplayHeight = (heightCm, isImperial) => {
  if (heightCm == null || heightCm === "") return "";
  const value = isImperial ? Number(heightCm) * CM_TO_IN : Number(heightCm);
  return isImperial ? String(Math.round(value)) : String(value);
};

const Settings = () => {
  const { t } = useTranslation();
  const toast = useToast();
  const { isDarkMode, toggleTheme } = useTheme();
  const {
    updateUnits,
    isImperial,
    convertWeightToMetric,
    convertHeightToMetric,
    weightUnit,
    heightUnit,
  } = useUnits();
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState("profile");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [user, setUser] = useState(null);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    bio: "",
    city: "",
    country: "",
    weight: "",
    height: "",
  });
  const [preferences, setPreferences] = useState({
    emailNotifications: true,
    pushNotifications: true,
    weeklySummary: true,
    units: "metric",
    defaultActivityType: "RUN",
    language: i18n.language || "en",
    darkMode: false,
  });
  const [passwordData, setPasswordData] = useState({
    currentPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [passwordErrors, setPasswordErrors] = useState({});
  const [changingPassword, setChangingPassword] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deletingAccount, setDeletingAccount] = useState(false);
  const [privacySettings, setPrivacySettings] = useState({
    privateProfile: false,
    hideFromSearch: false,
  });
  const [savingPrivacy, setSavingPrivacy] = useState(false);
  const [stravaStatus, setStravaStatus] = useState({
    connected: false,
    athleteId: null,
    connectedAt: null,
    lastImportedAt: null,
    activityCount: 0,
  });
  const [stravaBusy, setStravaBusy] = useState(false);

  useEffect(() => {
    const section = searchParams.get("section");
    if (section && VALID_SECTIONS.includes(section)) {
      setActiveTab(section);
    }
  }, [searchParams]);

  // Handle post-OAuth redirect flags and one-shot toast messages.
  useEffect(() => {
    const connected = searchParams.get("stravaConnected");
    const error = searchParams.get("stravaError");
    if (connected === "true") {
      toast.success(t("settings.account.integrations.strava.connectedToast"));
      setActiveTab("account");
    } else if (error) {
      toast.error(t("settings.account.integrations.strava.errorToast"));
      setActiveTab("account");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    loadUserData();
    loadPreferences();
    loadStravaStatus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setPreferences((prev) => ({ ...prev, darkMode: isDarkMode }));
  }, [isDarkMode]);

  // Re-convert displayed weight/height when the unit preference flips.
  // The `user` object holds the metric-stored values from the backend.
  useEffect(() => {
    if (!user) return;
    setFormData((prev) => ({
      ...prev,
      weight: user.weight != null ? formatDisplayWeight(user.weight, isImperial) : prev.weight,
      height: user.height != null ? formatDisplayHeight(user.height, isImperial) : prev.height,
    }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isImperial, user]);

  const loadUserData = async () => {
    try {
      setLoading(true);
      const response = await SettingsService.getCurrentUser();
      setUser(response.data);
      setFormData({
        firstName: response.data.firstName || "",
        lastName: response.data.lastName || "",
        bio: response.data.bio || "",
        city: response.data.city || "",
        country: response.data.country || "",
        weight: response.data.weight != null
          ? formatDisplayWeight(response.data.weight, isImperial)
          : "",
        height: response.data.height != null
          ? formatDisplayHeight(response.data.height, isImperial)
          : "",
      });
      // Load notification preferences from backend user data
      setPreferences((prev) => ({
        ...prev,
        emailNotifications: response.data.emailNotifications !== undefined ? response.data.emailNotifications : prev.emailNotifications,
        pushNotifications: response.data.pushNotifications !== undefined ? response.data.pushNotifications : prev.pushNotifications,
        weeklySummary: response.data.weeklySummary !== undefined ? response.data.weeklySummary : prev.weeklySummary,
      }));
      // Load privacy settings from backend user data
      setPrivacySettings({
        privateProfile: response.data.isPrivate === true,
        hideFromSearch: response.data.hideFromSearch === true,
      });
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  const loadPreferences = () => {
    const prefs = SettingsService.getPreferences();
    setPreferences((prev) => ({ ...prev, ...prefs }));
  };

  const loadStravaStatus = async () => {
    try {
      const res = await IntegrationService.getStravaStatus();
      setStravaStatus({
        connected: Boolean(res.data?.connected),
        athleteId: res.data?.athleteId ?? null,
        connectedAt: res.data?.connectedAt ?? null,
        lastImportedAt: res.data?.lastImportedAt ?? null,
        activityCount: res.data?.activityCount ?? 0,
      });
    } catch (e) {
      // Endpoint may 503 when credentials aren't configured — ignore silently.
    }
  };

  const handleStravaConnect = async () => {
    setStravaBusy(true);
    try {
      const res = await IntegrationService.getStravaAuthorizeUrl();
      if (res?.data?.url) {
        window.location.href = res.data.url;
      } else {
        toast.error(t("settings.account.integrations.strava.errorToast"));
      }
    } catch (e) {
      const status = e?.response?.status;
      if (status === 503) {
        toast.error(t("settings.account.integrations.strava.notConfigured"));
      } else {
        toast.error(t("settings.account.integrations.strava.errorToast"));
      }
    } finally {
      setStravaBusy(false);
    }
  };

  const handleStravaImport = async () => {
    setStravaBusy(true);
    try {
      await IntegrationService.importFromStrava();
      toast.success(t("settings.account.integrations.strava.importStarted"));
    } catch (e) {
      toast.error(t("settings.account.integrations.strava.errorToast"));
    } finally {
      setStravaBusy(false);
    }
  };

  const handleStravaDisconnect = async () => {
    setStravaBusy(true);
    try {
      await IntegrationService.disconnectStrava();
      setStravaStatus({
        connected: false,
        athleteId: null,
        connectedAt: null,
        lastImportedAt: null,
        activityCount: 0,
      });
      toast.success(t("settings.account.integrations.strava.disconnected"));
    } catch (e) {
      toast.error(t("settings.account.integrations.strava.errorToast"));
    } finally {
      setStravaBusy(false);
    }
  };

  const formatLastImported = (iso) => {
    if (!iso) return t("settings.account.integrations.strava.neverImported");
    try {
      return new Date(iso).toLocaleString();
    } catch (e) {
      return iso;
    }
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePreferenceChange = (key, value) => {
    setPreferences(prev => {
      const updated = { ...prev, [key]: value };
      // Persist to localStorage immediately so all components react
      SettingsService.savePreferences(updated);
      return updated;
    });
    if (key === "language") {
      i18n.changeLanguage(value);
    }
    if (key === "units") {
      updateUnits(value);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // Convert displayed weight/height back to metric for backend storage
      const weightKg = formData.weight
        ? convertWeightToMetric(parseFloat(formData.weight))
        : null;
      const heightCm = formData.height
        ? convertHeightToMetric(parseFloat(formData.height))
        : null;

      // Save profile data and notification preferences to backend
      const response = await SettingsService.updateProfile({
        firstName: formData.firstName,
        lastName: formData.lastName,
        bio: formData.bio || null,
        city: formData.city || null,
        country: formData.country || null,
        weight: weightKg,
        height: heightCm,
        emailNotifications: preferences.emailNotifications,
        pushNotifications: preferences.pushNotifications,
        weeklySummary: preferences.weeklySummary,
        isPrivate: privacySettings.privateProfile,
        hideFromSearch: privacySettings.hideFromSearch,
      });

      // Keep the metric-source-of-truth `user` state in sync with what we saved
      // so that a later unit flip re-converts from the correct metric value.
      setUser((prev) => ({
        ...(response?.data ?? prev ?? {}),
        weight: weightKg,
        height: heightCm,
      }));

      // Save all preferences (localStorage + backend notification prefs)
      await SettingsService.savePreferences(preferences);

      // Apply dark mode change if it differs from current theme
      if (preferences.darkMode !== isDarkMode) {
        toggleTheme();
      }

      toast.success(t("settings.saveSuccess"));
    } catch (error) {
      toast.error(t("settings.saveFailed"));
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData(prev => ({ ...prev, [name]: value }));
    setPasswordErrors(prev => ({ ...prev, [name]: "" }));
  };

  const validatePasswordForm = useCallback(() => {
    const errors = {};
    if (!passwordData.currentPassword) {
      errors.currentPassword = t("settings.account.passwordRequired");
    }
    if (!passwordData.newPassword) {
      errors.newPassword = t("settings.account.passwordRequired");
    } else if (passwordData.newPassword.length < 8) {
      errors.newPassword = t("settings.account.passwordMinLength");
    }
    if (!passwordData.confirmPassword) {
      errors.confirmPassword = t("settings.account.passwordRequired");
    } else if (passwordData.newPassword !== passwordData.confirmPassword) {
      errors.confirmPassword = t("settings.account.passwordsMismatch");
    }
    return errors;
  }, [passwordData, t]);

  const handleChangePassword = async () => {
    const errors = validatePasswordForm();
    if (Object.keys(errors).length > 0) {
      setPasswordErrors(errors);
      return;
    }
    setChangingPassword(true);
    try {
      await SettingsService.changePassword(passwordData.currentPassword, passwordData.newPassword);
      toast.success(t("settings.account.passwordChanged"));
      setPasswordData({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setPasswordErrors({});
    } catch (error) {
      const message = error.response?.data?.message || t("settings.account.passwordChangeFailed");
      toast.error(message);
    } finally {
      setChangingPassword(false);
    }
  };

  const handlePrivacyChange = async (key, value) => {
    const previous = privacySettings;
    const updated = { ...privacySettings, [key]: value };
    setPrivacySettings(updated);
    setSavingPrivacy(true);
    try {
      await SettingsService.updateProfile({
        firstName: formData.firstName,
        lastName: formData.lastName,
        bio: formData.bio || null,
        city: formData.city || null,
        country: formData.country || null,
        weight: formData.weight ? convertWeightToMetric(parseFloat(formData.weight)) : null,
        height: formData.height ? convertHeightToMetric(parseFloat(formData.height)) : null,
        emailNotifications: preferences.emailNotifications,
        pushNotifications: preferences.pushNotifications,
        weeklySummary: preferences.weeklySummary,
        isPrivate: updated.privateProfile,
        hideFromSearch: updated.hideFromSearch,
      });
      toast.success(t("settings.account.privacySaved"));
    } catch (error) {
      // Revert on failure
      setPrivacySettings(previous);
      toast.error(t("settings.account.privacySaveFailed"));
    } finally {
      setSavingPrivacy(false);
    }
  };

  const handleDeleteAccount = async () => {
    setDeletingAccount(true);
    try {
      await SettingsService.deleteAccount();
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userPreferences");
      localStorage.removeItem("privacySettings");
      window.location.href = "/login";
    } catch (error) {
      toast.error(t("settings.account.deleteFailed"));
      setShowDeleteModal(false);
    } finally {
      setDeletingAccount(false);
    }
  };

  const handleSignOut = () => {
    if (window.confirm(t("settings.confirmSignOut"))) {
      localStorage.removeItem("token");
      localStorage.removeItem("refreshToken");
      localStorage.removeItem("userPreferences");
      window.location.href = "/login";
    }
  };

  const getInitials = () => {
    if (!user) return "U";
    return `${(user.firstName || "")[0] || ""}${(user.lastName || "")[0] || ""}`.toUpperCase();
  };

  const tabs = [
    { id: "profile", label: t("settings.tabs.profile"), icon: <FiUser size={18} /> },
    { id: "notifications", label: t("settings.tabs.notifications"), icon: <FiBell size={18} /> },
    { id: "preferences", label: t("settings.tabs.preferences"), icon: <FiSliders size={18} /> },
    { id: "account", label: t("settings.tabs.account"), icon: <FiLock size={18} /> },
  ];

  if (loading) {
    return (
      <>
        <Navbar />
        <SettingsContainer>
          <SettingsContent>
            <LoadingSpinner />
          </SettingsContent>
        </SettingsContainer>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <SettingsContainer>
        <SettingsContent>
          <SettingsHeader>
            <SettingsTitle>{t("settings.title")}</SettingsTitle>
            <SaveButton onClick={handleSave} disabled={saving}>
              <FiSave size={18} />
              {saving ? t("settings.saving") : t("settings.save")}
            </SaveButton>
          </SettingsHeader>

          <SettingsLayout>
            <SettingsSidebar>
              {tabs.map(tab => (
                <SidebarItem
                  key={tab.id}
                  active={activeTab === tab.id}
                  onClick={() => setActiveTab(tab.id)}
                >
                  {tab.icon}
                  {tab.label}
                  <SidebarArrow active={activeTab === tab.id}>
                    <FiChevronRight size={16} />
                  </SidebarArrow>
                </SidebarItem>
              ))}
            </SettingsSidebar>

            <SettingsPanel>
              {activeTab === "profile" && (
                <>
                  <PanelTitle>{t("settings.profile.title")}</PanelTitle>
                  <PanelDescription>{t("settings.profile.description")}</PanelDescription>

                  <ProfileHeader>
                    <ProfileAvatar>
                      {user?.avatar ? (
                        <img src={user.avatar} alt="" />
                      ) : (
                        getInitials()
                      )}
                    </ProfileAvatar>
                    <ProfileInfo>
                      <ProfileName>{user?.firstName} {user?.lastName}</ProfileName>
                      <ProfileEmail>{user?.email}</ProfileEmail>
                    </ProfileInfo>
                  </ProfileHeader>

                  <FormGroup>
                    <FormLabel>{t("settings.profile.fullName")}</FormLabel>
                    <FormRow>
                      <FormInput
                        type="text"
                        name="firstName"
                        placeholder={t("settings.profile.firstNamePlaceholder")}
                        value={formData.firstName}
                        onChange={handleFormChange}
                      />
                      <FormInput
                        type="text"
                        name="lastName"
                        placeholder={t("settings.profile.lastNamePlaceholder")}
                        value={formData.lastName}
                        onChange={handleFormChange}
                      />
                    </FormRow>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>{t("settings.profile.bio")}</FormLabel>
                    <FormInput
                      type="text"
                      name="bio"
                      placeholder={t("settings.profile.bioPlaceholder")}
                      value={formData.bio}
                      onChange={handleFormChange}
                    />
                  </FormGroup>

                  <FormRow>
                    <FormGroup>
                      <FormLabel>{t("settings.profile.city")}</FormLabel>
                      <FormInput
                        type="text"
                        name="city"
                        placeholder={t("settings.profile.cityPlaceholder")}
                        value={formData.city}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                    <FormGroup>
                      <FormLabel>{t("settings.profile.country")}</FormLabel>
                      <FormInput
                        type="text"
                        name="country"
                        placeholder={t("settings.profile.countryPlaceholder")}
                        value={formData.country}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                  </FormRow>

                  <FormRow>
                    <FormGroup>
                      <FormLabel>{`${t("settings.profile.weight")} (${weightUnit})`}</FormLabel>
                      <FormInput
                        type="number"
                        name="weight"
                        placeholder={t("settings.profile.weightPlaceholder")}
                        value={formData.weight}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                    <FormGroup>
                      <FormLabel>{`${t("settings.profile.height")} (${heightUnit})`}</FormLabel>
                      <FormInput
                        type="number"
                        name="height"
                        placeholder={t("settings.profile.heightPlaceholder")}
                        value={formData.height}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                  </FormRow>
                </>
              )}

              {activeTab === "notifications" && (
                <>
                  <PanelTitle>{t("settings.notifications.title")}</PanelTitle>
                  <PanelDescription>{t("settings.notifications.description")}</PanelDescription>

                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>{t("settings.notifications.email")}</ToggleTitle>
                      <ToggleDescription>{t("settings.notifications.emailDesc")}</ToggleDescription>
                    </ToggleInfo>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={preferences.emailNotifications}
                        onChange={(e) => handlePreferenceChange("emailNotifications", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>

                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>{t("settings.notifications.toast")}</ToggleTitle>
                      <ToggleDescription>{t("settings.notifications.toastDesc")}</ToggleDescription>
                    </ToggleInfo>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={preferences.pushNotifications}
                        onChange={(e) => handlePreferenceChange("pushNotifications", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>

                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>{t("settings.notifications.weekly")}</ToggleTitle>
                      <ToggleDescription>{t("settings.notifications.weeklyDesc")}</ToggleDescription>
                    </ToggleInfo>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={preferences.weeklySummary}
                        onChange={(e) => handlePreferenceChange("weeklySummary", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>
                </>
              )}

              {activeTab === "preferences" && (
                <>
                  <PanelTitle>{t("settings.preferences.title")}</PanelTitle>
                  <PanelDescription>{t("settings.preferences.description")}</PanelDescription>

                  <FormGroup>
                    <FormLabel>{t("settings.preferences.units")}</FormLabel>
                    <FormSelect
                      value={preferences.units}
                      onChange={(e) => handlePreferenceChange("units", e.target.value)}
                    >
                      <option value="metric">{t("settings.preferences.metric")}</option>
                      <option value="imperial">{t("settings.preferences.imperial")}</option>
                    </FormSelect>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>{t("settings.preferences.defaultActivityType")}</FormLabel>
                    <FormSelect
                      value={preferences.defaultActivityType}
                      onChange={(e) => handlePreferenceChange("defaultActivityType", e.target.value)}
                    >
                      <option value="RUN">{t("activities.typesShort.RUN")}</option>
                      <option value="RIDE">{t("activities.typesShort.RIDE")}</option>
                      <option value="SWIM">{t("activities.typesShort.SWIM")}</option>
                      <option value="HIKE">{t("activities.typesShort.HIKE")}</option>
                      <option value="WALK">{t("activities.typesShort.WALK")}</option>
                    </FormSelect>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>{t("settings.preferences.language")}</FormLabel>
                    <FormSelect
                      value={preferences.language}
                      onChange={(e) => handlePreferenceChange("language", e.target.value)}
                    >
                      {SUPPORTED_LANGUAGES.map((lang) => (
                        <option key={lang.code} value={lang.code}>{lang.nativeName}</option>
                      ))}
                    </FormSelect>
                  </FormGroup>

                  <DarkModeToggle>
                    <DarkModeIcon>
                      <FiSun size={20} color="#f59e0b" />
                      <ToggleInfo>
                        <ToggleTitle>{t("settings.preferences.darkMode")}</ToggleTitle>
                        <ToggleDescription>{t("settings.preferences.darkModeDesc")}</ToggleDescription>
                      </ToggleInfo>
                    </DarkModeIcon>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={preferences.darkMode}
                        onChange={(e) => handlePreferenceChange("darkMode", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </DarkModeToggle>
                </>
              )}

              {activeTab === "account" && (
                <>
                  <PanelTitle>{t("settings.account.title")}</PanelTitle>
                  <PanelDescription>{t("settings.account.description")}</PanelDescription>

                  <AccountEmail>
                    <AccountEmailLabel>{t("settings.account.email")}</AccountEmailLabel>
                    <AccountEmailValue>{user?.email}</AccountEmailValue>
                  </AccountEmail>

                  <SectionDivider />

                  <SectionTitle>{t("settings.account.changePassword")}</SectionTitle>
                  <FormGroup>
                    <FormLabel>{t("settings.account.currentPassword")}</FormLabel>
                    <FormInput
                      type="password"
                      name="currentPassword"
                      placeholder={t("settings.account.currentPasswordPlaceholder")}
                      value={passwordData.currentPassword}
                      onChange={handlePasswordChange}
                    />
                    {passwordErrors.currentPassword && <FormError>{passwordErrors.currentPassword}</FormError>}
                  </FormGroup>
                  <FormGroup>
                    <FormLabel>{t("settings.account.newPassword")}</FormLabel>
                    <FormInput
                      type="password"
                      name="newPassword"
                      placeholder={t("settings.account.newPasswordPlaceholder")}
                      value={passwordData.newPassword}
                      onChange={handlePasswordChange}
                    />
                    {passwordErrors.newPassword && <FormError>{passwordErrors.newPassword}</FormError>}
                  </FormGroup>
                  <FormGroup>
                    <FormLabel>{t("settings.account.confirmNewPassword")}</FormLabel>
                    <FormInput
                      type="password"
                      name="confirmPassword"
                      placeholder={t("settings.account.confirmNewPasswordPlaceholder")}
                      value={passwordData.confirmPassword}
                      onChange={handlePasswordChange}
                    />
                    {passwordErrors.confirmPassword && <FormError>{passwordErrors.confirmPassword}</FormError>}
                  </FormGroup>
                  <PasswordFormButton onClick={handleChangePassword} disabled={changingPassword}>
                    <FiLock size={16} />
                    {changingPassword ? t("settings.account.changingPassword") : t("settings.account.changePassword")}
                  </PasswordFormButton>

                  <SectionDivider />

                  <SectionTitle>{t("settings.account.privacyTitle")}</SectionTitle>
                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>{t("settings.account.privateProfile")}</ToggleTitle>
                      <ToggleDescription>{t("settings.account.privateProfileDesc")}</ToggleDescription>
                    </ToggleInfo>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={privacySettings.privateProfile}
                        disabled={savingPrivacy}
                        onChange={(e) => handlePrivacyChange("privateProfile", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>
                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>{t("settings.account.hideFromSearch")}</ToggleTitle>
                      <ToggleDescription>{t("settings.account.hideFromSearchDesc")}</ToggleDescription>
                    </ToggleInfo>
                    <Toggle>
                      <input
                        type="checkbox"
                        checked={privacySettings.hideFromSearch}
                        disabled={savingPrivacy}
                        onChange={(e) => handlePrivacyChange("hideFromSearch", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>

                  <SectionDivider />

                  <SectionTitle>{t("settings.account.integrations.title")}</SectionTitle>
                  <PanelDescription>{t("settings.account.integrations.description")}</PanelDescription>
                  <IntegrationRow>
                    <IntegrationInfo>
                      <IntegrationName>
                        {t("settings.account.integrations.strava.name")}
                      </IntegrationName>
                      {stravaStatus.connected ? (
                        <>
                          <IntegrationDetail>
                            {t("settings.account.integrations.strava.athleteId")}: {stravaStatus.athleteId ?? "-"}
                          </IntegrationDetail>
                          <IntegrationDetail>
                            {t("settings.account.integrations.strava.lastImported")}: {formatLastImported(stravaStatus.lastImportedAt)}
                          </IntegrationDetail>
                          <IntegrationDetail>
                            {t("settings.account.integrations.strava.importedCount", { count: stravaStatus.activityCount })}
                          </IntegrationDetail>
                        </>
                      ) : (
                        <IntegrationDetail>
                          {t("settings.account.integrations.strava.notConnected")}
                        </IntegrationDetail>
                      )}
                    </IntegrationInfo>
                    <IntegrationActions>
                      {stravaStatus.connected ? (
                        <>
                          <StravaConnectButton onClick={handleStravaImport} disabled={stravaBusy}>
                            <FiDownloadCloud size={16} />
                            {t("settings.account.integrations.strava.importNow")}
                          </StravaConnectButton>
                          <SecondaryButton onClick={handleStravaDisconnect} disabled={stravaBusy}>
                            <FiZap size={14} />
                            {t("settings.account.integrations.strava.disconnect")}
                          </SecondaryButton>
                        </>
                      ) : (
                        <StravaConnectButton onClick={handleStravaConnect} disabled={stravaBusy}>
                          <FiLink size={16} />
                          {t("settings.account.integrations.strava.connect")}
                        </StravaConnectButton>
                      )}
                    </IntegrationActions>
                  </IntegrationRow>

                  <SectionDivider />

                  <DangerZone>
                    <DangerZoneTitle>{t("settings.account.dangerZone")}</DangerZoneTitle>
                    <DangerZoneDescription>{t("settings.account.dangerZoneDesc")}</DangerZoneDescription>
                    <DangerButton onClick={() => setShowDeleteModal(true)}>
                      <FiTrash2 size={16} />
                      {t("settings.account.deleteAccount")}
                    </DangerButton>
                  </DangerZone>

                  <SignOutButton onClick={handleSignOut}>
                    <FiLogOut size={18} />
                    {t("nav.signOut")}
                  </SignOutButton>

                  {showDeleteModal && (
                    <ModalOverlay onClick={() => setShowDeleteModal(false)}>
                      <ModalContent onClick={(e) => e.stopPropagation()}>
                        <ModalTitle>{t("settings.account.deleteAccountTitle")}</ModalTitle>
                        <ModalText>{t("settings.account.deleteAccountConfirm")}</ModalText>
                        <ModalActions>
                          <ModalCancelButton onClick={() => setShowDeleteModal(false)}>
                            {t("common.cancel")}
                          </ModalCancelButton>
                          <DangerButton onClick={handleDeleteAccount} disabled={deletingAccount}>
                            <FiTrash2 size={16} />
                            {deletingAccount ? t("common.deleting") : t("settings.account.deleteMyAccount")}
                          </DangerButton>
                        </ModalActions>
                      </ModalContent>
                    </ModalOverlay>
                  )}
                </>
              )}
            </SettingsPanel>
          </SettingsLayout>
        </SettingsContent>
      </SettingsContainer>
    </>
  );
};

export default Settings;
