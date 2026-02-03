import React, { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { FiPlus, FiBell, FiSearch, FiSun, FiMoon, FiMenu, FiX, FiHome, FiActivity, FiAward, FiUsers, FiCalendar, FiUser, FiTarget, FiBook, FiSettings, FiShield, FiLogOut } from "react-icons/fi";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import {
  Nav,
  Logo,
  LogoIcon,
  LogoText,
  NavLinks,
  NavLink,
  NavRight,
  IconButton,
  HideOnMobile,
  MobileMenuButton,
  Avatar,
  AvatarWrapper,
  DropdownMenu,
  DropdownItem,
  DropdownButton,
  MobileMenuOverlay,
  MobileMenu,
  MobileNavLink,
  MobileDivider,
} from "./NavbarStyles";

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const [showDropdown, setShowDropdown] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path) => location.pathname === path;

  // Close mobile menu on route change
  useEffect(() => {
    setMobileMenuOpen(false);
    setShowDropdown(false);
  }, [location.pathname]);

  // Prevent body scroll when mobile menu is open
  useEffect(() => {
    if (mobileMenuOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [mobileMenuOpen]);

  const handleLogout = () => {
    logout();
    navigate("/login");
    setShowDropdown(false);
    setMobileMenuOpen(false);
  };

  const getInitials = () => {
    if (!user) return "??";
    const first = user.firstName?.[0] || "";
    const last = user.lastName?.[0] || "";
    return (first + last).toUpperCase() || "??";
  };

  return (
    <>
      <Nav>
        <Logo to="/">
          <LogoIcon>🏃</LogoIcon>
          <LogoText>Strava Clone</LogoText>
        </Logo>

        <NavLinks>
          <NavLink to="/" className={isActive("/") ? "active" : ""}>
            Dashboard
          </NavLink>
          <NavLink to="/activities" className={isActive("/activities") ? "active" : ""}>
            Activities
          </NavLink>
          <NavLink to="/challenges" className={isActive("/challenges") ? "active" : ""}>
            Challenges
          </NavLink>
          <NavLink to="/clubs" className={isActive("/clubs") ? "active" : ""}>
            Clubs
          </NavLink>
          <NavLink to="/events" className={isActive("/events") ? "active" : ""}>
            Events
          </NavLink>
        </NavLinks>

        <NavRight>
          <HideOnMobile>
            <IconButton title="Search">
              <FiSearch size={20} />
            </IconButton>
          </HideOnMobile>
          <IconButton title="Add Activity" onClick={() => navigate("/activities/new")}>
            <FiPlus size={22} />
          </IconButton>
          <HideOnMobile>
            <IconButton title="Notifications">
              <FiBell size={20} />
            </IconButton>
            <IconButton title={isDarkMode ? "Light Mode" : "Dark Mode"} onClick={toggleTheme}>
              {isDarkMode ? <FiSun size={20} /> : <FiMoon size={20} />}
            </IconButton>
          </HideOnMobile>
          <AvatarWrapper>
            <Avatar onClick={() => setShowDropdown(!showDropdown)}>
              {user?.profileImageUrl ? (
                <img src={user.profileImageUrl} alt="Profile" />
              ) : (
                getInitials()
              )}
            </Avatar>
            {showDropdown && (
              <DropdownMenu>
                <DropdownItem to="/profile" onClick={() => setShowDropdown(false)}>
                  My Profile
                </DropdownItem>
                <DropdownItem to="/goals" onClick={() => setShowDropdown(false)}>
                  Goals
                </DropdownItem>
                <DropdownItem to="/training" onClick={() => setShowDropdown(false)}>
                  Training Plans
                </DropdownItem>
                <DropdownItem to="/admin" onClick={() => setShowDropdown(false)}>
                  Admin Panel
                </DropdownItem>
                <DropdownItem to="/settings" onClick={() => setShowDropdown(false)}>
                  Settings
                </DropdownItem>
                <DropdownButton onClick={handleLogout}>Log Out</DropdownButton>
              </DropdownMenu>
            )}
          </AvatarWrapper>
          <MobileMenuButton
            title="Menu"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <FiX size={24} /> : <FiMenu size={24} />}
          </MobileMenuButton>
        </NavRight>
      </Nav>

      {/* Mobile Menu */}
      <MobileMenuOverlay $isOpen={mobileMenuOpen} onClick={() => setMobileMenuOpen(false)} />
      <MobileMenu $isOpen={mobileMenuOpen}>
        <MobileNavLink to="/" className={isActive("/") ? "active" : ""}>
          <FiHome /> Dashboard
        </MobileNavLink>
        <MobileNavLink to="/activities" className={isActive("/activities") ? "active" : ""}>
          <FiActivity /> Activities
        </MobileNavLink>
        <MobileNavLink to="/challenges" className={isActive("/challenges") ? "active" : ""}>
          <FiAward /> Challenges
        </MobileNavLink>
        <MobileNavLink to="/clubs" className={isActive("/clubs") ? "active" : ""}>
          <FiUsers /> Clubs
        </MobileNavLink>
        <MobileNavLink to="/events" className={isActive("/events") ? "active" : ""}>
          <FiCalendar /> Events
        </MobileNavLink>

        <MobileDivider />

        <MobileNavLink to="/profile" className={isActive("/profile") ? "active" : ""}>
          <FiUser /> My Profile
        </MobileNavLink>
        <MobileNavLink to="/goals" className={isActive("/goals") ? "active" : ""}>
          <FiTarget /> Goals
        </MobileNavLink>
        <MobileNavLink to="/training" className={isActive("/training") ? "active" : ""}>
          <FiBook /> Training Plans
        </MobileNavLink>
        <MobileNavLink to="/admin" className={isActive("/admin") ? "active" : ""}>
          <FiShield /> Admin Panel
        </MobileNavLink>
        <MobileNavLink to="/settings" className={isActive("/settings") ? "active" : ""}>
          <FiSettings /> Settings
        </MobileNavLink>

        <MobileDivider />

        <MobileNavLink as="button" onClick={toggleTheme} style={{ width: "100%", border: "none", background: "none", cursor: "pointer" }}>
          {isDarkMode ? <FiSun /> : <FiMoon />}
          {isDarkMode ? "Light Mode" : "Dark Mode"}
        </MobileNavLink>
        <MobileNavLink as="button" onClick={handleLogout} style={{ width: "100%", border: "none", background: "none", cursor: "pointer", color: "#ef4444" }}>
          <FiLogOut /> Log Out
        </MobileNavLink>
      </MobileMenu>
    </>
  );
};

export default Navbar;
