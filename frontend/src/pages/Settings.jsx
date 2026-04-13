import React, { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiUser, FiBell, FiSliders, FiLock, FiChevronRight, FiSave, FiLogOut, FiSun, FiTrash2 } from "react-icons/fi";
import Navbar from "../components/Navbar/Navbar";
import SettingsService from "../api/SettingsService";
import { useToast } from "../context/ToastContext";
import { useTheme } from "../context/ThemeContext";
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
} from "./SettingsStyles";

const VALID_SECTIONS = ["profile", "notifications", "preferences", "account"];

const Settings = () => {
  const { t } = useTranslation();
  const toast = useToast();
  const { isDarkMode, toggleTheme } = useTheme();
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
    paceDisplay: "pace",
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

  useEffect(() => {
    const section = searchParams.get("section");
    if (section && VALID_SECTIONS.includes(section)) {
      setActiveTab(section);
    }
  }, [searchParams]);

  useEffect(() => {
    loadUserData();
    loadPreferences();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setPreferences((prev) => ({ ...prev, darkMode: isDarkMode }));
  }, [isDarkMode]);

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
        weight: response.data.weight || "",
        height: response.data.height || "",
      });
      // Load notification preferences from backend user data
      setPreferences((prev) => ({
        ...prev,
        emailNotifications: response.data.emailNotifications !== undefined ? response.data.emailNotifications : prev.emailNotifications,
        pushNotifications: response.data.pushNotifications !== undefined ? response.data.pushNotifications : prev.pushNotifications,
        weeklySummary: response.data.weeklySummary !== undefined ? response.data.weeklySummary : prev.weeklySummary,
      }));
    } catch (error) {
    } finally {
      setLoading(false);
    }
  };

  const loadPreferences = () => {
    const prefs = SettingsService.getPreferences();
    setPreferences((prev) => ({ ...prev, ...prefs }));
  };

  useEffect(() => {
    const stored = localStorage.getItem("privacySettings");
    if (stored) {
      setPrivacySettings(JSON.parse(stored));
    }
  }, []);

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePreferenceChange = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }));
    if (key === "language") {
      i18n.changeLanguage(value);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // Save profile data and notification preferences to backend
      await SettingsService.updateProfile({
        firstName: formData.firstName,
        lastName: formData.lastName,
        bio: formData.bio || null,
        city: formData.city || null,
        country: formData.country || null,
        weight: formData.weight ? parseFloat(formData.weight) : null,
        height: formData.height ? parseFloat(formData.height) : null,
        emailNotifications: preferences.emailNotifications,
        pushNotifications: preferences.pushNotifications,
        weeklySummary: preferences.weeklySummary,
      });

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

  const handlePrivacyChange = (key, value) => {
    const updated = { ...privacySettings, [key]: value };
    setPrivacySettings(updated);
    localStorage.setItem("privacySettings", JSON.stringify(updated));
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
                      <FormLabel>{t("settings.profile.weight")}</FormLabel>
                      <FormInput
                        type="number"
                        name="weight"
                        placeholder={t("settings.profile.weightPlaceholder")}
                        value={formData.weight}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                    <FormGroup>
                      <FormLabel>{t("settings.profile.height")}</FormLabel>
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
                    <FormLabel>{t("settings.preferences.paceDisplay")}</FormLabel>
                    <FormSelect
                      value={preferences.paceDisplay}
                      onChange={(e) => handlePreferenceChange("paceDisplay", e.target.value)}
                    >
                      <option value="pace">{t("settings.preferences.pace")}</option>
                      <option value="speed">{t("settings.preferences.speed")}</option>
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
                        onChange={(e) => handlePrivacyChange("hideFromSearch", e.target.checked)}
                      />
                      <span></span>
                    </Toggle>
                  </ToggleItem>

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
