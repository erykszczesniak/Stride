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
} from "react-icons/fi";
import { RiAdminLine } from "react-icons/ri";
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
} from "./SidebarStyles";

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();

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

  const bottomNavItems = [
    { path: "/settings", label: "Settings", icon: FiSettings },
    { path: "/logout", label: "Sign Out", icon: FiLogOut },
  ];

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
        {bottomNavItems.map((item) => (
          <NavItem
            key={item.path}
            to={item.path}
            className={isActive(item.path) ? "active" : ""}
          >
            <NavIcon>
              <item.icon size={20} />
            </NavIcon>
            <NavLabel>{item.label}</NavLabel>
          </NavItem>
        ))}
      </BottomSection>
    </SidebarContainer>
  );
};

export default Sidebar;
