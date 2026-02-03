import React from "react";
import { useLocation, useNavigate } from "react-router-dom";
import {
  FiHome,
  FiActivity,
  FiUser,
  FiTarget,
  FiCalendar,
  FiUsers,
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
  const location = useLocation();
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();

  const mainNavItems = [
    { path: "/", label: "Dashboard", icon: FiHome },
    { path: "/activities", label: "Activities", icon: FiActivity },
    { path: "/profile", label: "Profile", icon: FiUser },
    { path: "/challenges", label: "Challenges", icon: FiAward },
    { path: "/training", label: "Training Plans", icon: FiTarget },
    { path: "/clubs", label: "Clubs", icon: FiUsers },
    { path: "/events", label: "Events", icon: FiCalendar },
    { path: "/admin", label: "Admin Panel", icon: RiAdminLine },
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
        Record Activity
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
        <ThemeToggle onClick={toggleTheme} title={isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}>
          <NavIcon>
            {isDarkMode ? <FiSun size={20} /> : <FiMoon size={20} />}
          </NavIcon>
          <NavLabel>{isDarkMode ? "Light Mode" : "Dark Mode"}</NavLabel>
        </ThemeToggle>
        <NavItem
          to="/settings"
          className={isActive("/settings") ? "active" : ""}
        >
          <NavIcon>
            <FiSettings size={20} />
          </NavIcon>
          <NavLabel>Settings</NavLabel>
        </NavItem>
        <LogoutButton onClick={handleLogout}>
          <NavIcon>
            <FiLogOut size={20} />
          </NavIcon>
          <NavLabel>Sign Out</NavLabel>
        </LogoutButton>
      </BottomSection>
    </SidebarContainer>
  );
};

export default Sidebar;
