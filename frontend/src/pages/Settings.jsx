import React, { useState, useEffect } from "react";
import { FiUser, FiBell, FiSliders, FiLock, FiChevronRight, FiSave, FiLogOut, FiSun } from "react-icons/fi";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking, FaWalking } from "react-icons/fa";
import Navbar from "../components/Navbar/Navbar";
import SettingsService from "../api/SettingsService";
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
} from "./SettingsStyles";

const Settings = () => {
  const [activeTab, setActiveTab] = useState("profile");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [user, setUser] = useState(null);
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
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
    language: "en",
    darkMode: false,
  });

  useEffect(() => {
    loadUserData();
    loadPreferences();
  }, []);

  const loadUserData = async () => {
    try {
      setLoading(true);
      const response = await SettingsService.getCurrentUser(1);
      setUser(response.data);
      setFormData({
        firstName: response.data.firstName || "",
        lastName: response.data.lastName || "",
        weight: response.data.weight || "",
        height: response.data.height || "",
      });
    } catch (error) {
      console.error("Error loading user data:", error);
    } finally {
      setLoading(false);
    }
  };

  const loadPreferences = () => {
    const prefs = SettingsService.getPreferences();
    setPreferences(prefs);
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handlePreferenceChange = (key, value) => {
    setPreferences(prev => ({ ...prev, [key]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      // Save profile data to backend
      await SettingsService.updateProfile(1, {
        firstName: formData.firstName,
        lastName: formData.lastName,
        weight: formData.weight ? parseFloat(formData.weight) : null,
        height: formData.height ? parseFloat(formData.height) : null,
      });

      // Save preferences to localStorage
      await SettingsService.savePreferences(preferences);

      alert("Settings saved successfully!");
    } catch (error) {
      console.error("Error saving settings:", error);
      alert("Failed to save settings. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  const handleSignOut = () => {
    if (window.confirm("Are you sure you want to sign out?")) {
      localStorage.removeItem("token");
      localStorage.removeItem("userPreferences");
      window.location.href = "/";
    }
  };

  const getInitials = () => {
    if (!user) return "U";
    return `${(user.firstName || "")[0] || ""}${(user.lastName || "")[0] || ""}`.toUpperCase();
  };

  const tabs = [
    { id: "profile", label: "Profile", icon: <FiUser size={18} /> },
    { id: "notifications", label: "Notifications", icon: <FiBell size={18} /> },
    { id: "preferences", label: "Preferences", icon: <FiSliders size={18} /> },
    { id: "account", label: "Account", icon: <FiLock size={18} /> },
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
            <SettingsTitle>Settings</SettingsTitle>
            <SaveButton onClick={handleSave} disabled={saving}>
              <FiSave size={18} />
              {saving ? "Saving..." : "Save Changes"}
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
                  <PanelTitle>Profile</PanelTitle>
                  <PanelDescription>Manage your personal information</PanelDescription>

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
                    <FormLabel>Full Name</FormLabel>
                    <FormRow>
                      <FormInput
                        type="text"
                        name="firstName"
                        placeholder="First Name"
                        value={formData.firstName}
                        onChange={handleFormChange}
                      />
                      <FormInput
                        type="text"
                        name="lastName"
                        placeholder="Last Name"
                        value={formData.lastName}
                        onChange={handleFormChange}
                      />
                    </FormRow>
                  </FormGroup>

                  <FormRow>
                    <FormGroup>
                      <FormLabel>Weight (kg)</FormLabel>
                      <FormInput
                        type="number"
                        name="weight"
                        placeholder="Enter weight"
                        value={formData.weight}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                    <FormGroup>
                      <FormLabel>Height (cm)</FormLabel>
                      <FormInput
                        type="number"
                        name="height"
                        placeholder="Enter height"
                        value={formData.height}
                        onChange={handleFormChange}
                      />
                    </FormGroup>
                  </FormRow>
                </>
              )}

              {activeTab === "notifications" && (
                <>
                  <PanelTitle>Notifications</PanelTitle>
                  <PanelDescription>Configure how you receive updates</PanelDescription>

                  <ToggleItem>
                    <ToggleInfo>
                      <ToggleTitle>Email Notifications</ToggleTitle>
                      <ToggleDescription>Receive updates via email</ToggleDescription>
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
                      <ToggleTitle>Push Notifications</ToggleTitle>
                      <ToggleDescription>Receive push notifications</ToggleDescription>
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
                      <ToggleTitle>Weekly Summary</ToggleTitle>
                      <ToggleDescription>Get a weekly training report</ToggleDescription>
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
                  <PanelTitle>Preferences</PanelTitle>
                  <PanelDescription>Customize your experience</PanelDescription>

                  <FormGroup>
                    <FormLabel>Units</FormLabel>
                    <FormSelect
                      value={preferences.units}
                      onChange={(e) => handlePreferenceChange("units", e.target.value)}
                    >
                      <option value="metric">Metric (km, kg)</option>
                      <option value="imperial">Imperial (mi, lb)</option>
                    </FormSelect>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>Default Activity Type</FormLabel>
                    <FormSelect
                      value={preferences.defaultActivityType}
                      onChange={(e) => handlePreferenceChange("defaultActivityType", e.target.value)}
                    >
                      <option value="RUN">Run</option>
                      <option value="RIDE">Ride</option>
                      <option value="SWIM">Swim</option>
                      <option value="HIKE">Hike</option>
                      <option value="WALK">Walk</option>
                    </FormSelect>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>Pace/Speed Display</FormLabel>
                    <FormSelect
                      value={preferences.paceDisplay}
                      onChange={(e) => handlePreferenceChange("paceDisplay", e.target.value)}
                    >
                      <option value="pace">Pace (min/km or min/mi)</option>
                      <option value="speed">Speed (km/h or mph)</option>
                    </FormSelect>
                  </FormGroup>

                  <FormGroup>
                    <FormLabel>Language</FormLabel>
                    <FormSelect
                      value={preferences.language}
                      onChange={(e) => handlePreferenceChange("language", e.target.value)}
                    >
                      <option value="en">English</option>
                      <option value="pl">Polish</option>
                      <option value="de">German</option>
                      <option value="fr">French</option>
                      <option value="es">Spanish</option>
                    </FormSelect>
                  </FormGroup>

                  <DarkModeToggle>
                    <DarkModeIcon>
                      <FiSun size={20} color="#f59e0b" />
                      <ToggleInfo>
                        <ToggleTitle>Dark Mode</ToggleTitle>
                        <ToggleDescription>Use dark theme</ToggleDescription>
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
                  <PanelTitle>Account</PanelTitle>
                  <PanelDescription>Manage your account settings</PanelDescription>

                  <AccountEmail>
                    <AccountEmailLabel>Email</AccountEmailLabel>
                    <AccountEmailValue>{user?.email}</AccountEmailValue>
                  </AccountEmail>

                  <SignOutButton onClick={handleSignOut}>
                    <FiLogOut size={18} />
                    Sign Out
                  </SignOutButton>
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
