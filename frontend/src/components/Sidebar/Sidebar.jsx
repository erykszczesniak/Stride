import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  FiHome,
  FiActivity,
  FiUser,
  FiTarget,
  FiClipboard,
  FiCalendar,
  FiUsers,
  FiFileText,
  FiSettings,
  FiLogOut,
  FiPlus,
  FiAward,
  FiSun,
  FiMoon,
} from "react-icons/fi";
import { RiAdminLine } from "react-icons/ri";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import {
  SidebarContainer,
  Logo,
  LogoIcon,
  LogoText,
  RecordButton,
  NavSection,
  NavItem,
  NavIcon,
  NavLabel,
  ActiveDot,
  BottomSection,
  LogoutButton,
  ThemeToggle,
} from "./SidebarStyles";

const Sidebar = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();

  const mainNavItems = [
    { path: "/", label: t("nav.dashboard"), icon: FiHome },
    { path: "/activities", label: t("nav.activities"), icon: FiActivity },
    { path: "/profile", label: t("nav.profile"), icon: FiUser },
    { path: "/goals", label: t("nav.goals"), icon: FiTarget },
    { path: "/challenges", label: t("nav.challenges"), icon: FiAward },
    { path: "/training", label: t("nav.trainingPlans"), icon: FiClipboard },
    { path: "/clubs", label: t("nav.clubs"), icon: FiUsers },
    { path: "/events", label: t("nav.events"), icon: FiCalendar },
    { path: "/reports", label: t("nav.reports"), icon: FiFileText },
    ...(user?.role === "ADMIN" ? [{ path: "/admin", label: t("nav.adminPanel"), icon: RiAdminLine }] : []),
  ];

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const isActive = (path) => {
    if (path === "/") return location.pathname === "/";
    return location.pathname.startsWith(path);
  };

  return (
    <SidebarContainer>
      <Logo to="/">
        <LogoIcon>
          <FiActivity size={24} />
        </LogoIcon>
        <LogoText>STRIDE</LogoText>
      </Logo>

      <RecordButton onClick={() => navigate("/activities/new")}>
        <FiPlus size={18} />
        {t("nav.recordActivity")}
      </RecordButton>

      <NavSection>
        {mainNavItems.map((item) => (
          <NavItem
            key={item.path}
            to={item.path}
            className={isActive(item.path) ? "active" : ""}
          >
            <NavIcon>
              <item.icon size={20} />
            </NavIcon>
            <NavLabel>{item.label}</NavLabel>
            {isActive(item.path) && <ActiveDot />}
          </NavItem>
        ))}
      </NavSection>

      <BottomSection>
        <ThemeToggle onClick={toggleTheme} title={isDarkMode ? t("nav.switchToLightMode") : t("nav.switchToDarkMode")}>
          <NavIcon>
            {isDarkMode ? <FiSun size={20} /> : <FiMoon size={20} />}
          </NavIcon>
          <NavLabel>{isDarkMode ? t("nav.lightMode") : t("nav.darkMode")}</NavLabel>
        </ThemeToggle>
        <NavItem
          to="/settings"
          className={isActive("/settings") ? "active" : ""}
        >
          <NavIcon>
            <FiSettings size={20} />
          </NavIcon>
          <NavLabel>{t("nav.settings")}</NavLabel>
        </NavItem>
        <LogoutButton onClick={handleLogout}>
          <NavIcon>
            <FiLogOut size={20} />
          </NavIcon>
          <NavLabel>{t("nav.signOut")}</NavLabel>
        </LogoutButton>
      </BottomSection>
    </SidebarContainer>
  );
};

export default Sidebar;
