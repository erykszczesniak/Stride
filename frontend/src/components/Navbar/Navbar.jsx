import React, { useState, useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiPlus, FiSearch, FiSun, FiMoon, FiMenu, FiX, FiHome, FiActivity, FiAward, FiUsers, FiCalendar, FiUser, FiTarget, FiBook, FiSettings, FiShield, FiLogOut } from "react-icons/fi";
import { useAuth } from "../../context/AuthContext";
import { useTheme } from "../../context/ThemeContext";
import SearchModal from "../SearchModal/SearchModal";
import NotificationDropdown from "./NotificationDropdown";
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
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const [showDropdown, setShowDropdown] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [showSearch, setShowSearch] = useState(false);

  const isActive = (path) => location.pathname === path;

  // On the Settings page the navbar strips down to just search + theme + avatar
  // so the user isn't distracted by the add-activity / notifications shortcuts
  // while they're configuring their account.
  const isSettingsPage = location.pathname.startsWith("/settings");

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

  // Cmd+K / Ctrl+K keyboard shortcut to open search
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        setShowSearch((prev) => !prev);
      }
    };
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, []);

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
      <Nav role="navigation" aria-label={t("nav.mainNavigation")}>
        <Logo to="/">
          <LogoIcon><FiActivity size={20} /></LogoIcon>
          <LogoText>STRIDE</LogoText>
        </Logo>

        <NavLinks>
          <NavLink to="/" className={isActive("/") ? "active" : ""}>
            {t("nav.dashboard")}
          </NavLink>
          <NavLink to="/activities" className={isActive("/activities") ? "active" : ""}>
            {t("nav.activities")}
          </NavLink>
          <NavLink to="/challenges" className={isActive("/challenges") ? "active" : ""}>
            {t("nav.challenges")}
          </NavLink>
          <NavLink to="/clubs" className={isActive("/clubs") ? "active" : ""}>
            {t("nav.clubs")}
          </NavLink>
          <NavLink to="/events" className={isActive("/events") ? "active" : ""}>
            {t("nav.events")}
          </NavLink>
        </NavLinks>

        <NavRight>
          {/* Search is visible at every breakpoint — there's no way to trigger the
              Cmd/Ctrl+K shortcut on mobile, so the magnifier icon is the only way
              to open the command palette on a phone. */}
          <IconButton title={t("nav.searchShortcut")} onClick={() => setShowSearch(true)}>
            <FiSearch size={20} />
          </IconButton>
          {!isSettingsPage && (
            <IconButton title={t("nav.addActivity")} onClick={() => navigate("/activities/new")}>
              <FiPlus size={22} />
            </IconButton>
          )}
          <HideOnMobile>
            {!isSettingsPage && <NotificationDropdown />}
            <IconButton title={isDarkMode ? t("nav.lightMode") : t("nav.darkMode")} onClick={toggleTheme}>
              {isDarkMode ? <FiSun size={20} /> : <FiMoon size={20} />}
            </IconButton>
          </HideOnMobile>
          <AvatarWrapper>
            <Avatar onClick={() => setShowDropdown(!showDropdown)}>
              {user?.profileImageUrl ? (
                <img src={user.profileImageUrl} alt={t("nav.profile")} />
              ) : (
                getInitials()
              )}
            </Avatar>
            {showDropdown && (
              <DropdownMenu>
                <DropdownItem to="/profile" onClick={() => setShowDropdown(false)}>
                  {t("nav.myProfile")}
                </DropdownItem>
                <DropdownItem to="/goals" onClick={() => setShowDropdown(false)}>
                  {t("nav.goals")}
                </DropdownItem>
                <DropdownItem to="/training" onClick={() => setShowDropdown(false)}>
                  {t("nav.trainingPlans")}
                </DropdownItem>
                {user?.role === "ADMIN" && (
                  <DropdownItem to="/admin" onClick={() => setShowDropdown(false)}>
                    {t("nav.adminPanel")}
                  </DropdownItem>
                )}
                <DropdownItem to="/settings" onClick={() => setShowDropdown(false)}>
                  {t("nav.settings")}
                </DropdownItem>
                <DropdownButton onClick={handleLogout}>{t("nav.logout")}</DropdownButton>
              </DropdownMenu>
            )}
          </AvatarWrapper>
          <MobileMenuButton
            title={t("nav.menu")}
            aria-label={mobileMenuOpen ? t("nav.closeMenu") : t("nav.openMenu")}
            aria-expanded={mobileMenuOpen}
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
          <FiHome /> {t("nav.dashboard")}
        </MobileNavLink>
        <MobileNavLink to="/activities" className={isActive("/activities") ? "active" : ""}>
          <FiActivity /> {t("nav.activities")}
        </MobileNavLink>
        <MobileNavLink to="/challenges" className={isActive("/challenges") ? "active" : ""}>
          <FiAward /> {t("nav.challenges")}
        </MobileNavLink>
        <MobileNavLink to="/clubs" className={isActive("/clubs") ? "active" : ""}>
          <FiUsers /> {t("nav.clubs")}
        </MobileNavLink>
        <MobileNavLink to="/events" className={isActive("/events") ? "active" : ""}>
          <FiCalendar /> {t("nav.events")}
        </MobileNavLink>

        <MobileDivider />

        <MobileNavLink to="/profile" className={isActive("/profile") ? "active" : ""}>
          <FiUser /> {t("nav.myProfile")}
        </MobileNavLink>
        <MobileNavLink to="/goals" className={isActive("/goals") ? "active" : ""}>
          <FiTarget /> {t("nav.goals")}
        </MobileNavLink>
        <MobileNavLink to="/training" className={isActive("/training") ? "active" : ""}>
          <FiBook /> {t("nav.trainingPlans")}
        </MobileNavLink>
        {user?.role === "ADMIN" && (
          <MobileNavLink to="/admin" className={isActive("/admin") ? "active" : ""}>
            <FiShield /> {t("nav.adminPanel")}
          </MobileNavLink>
        )}
        <MobileNavLink to="/settings" className={isActive("/settings") ? "active" : ""}>
          <FiSettings /> {t("nav.settings")}
        </MobileNavLink>

        <MobileDivider />

        <MobileNavLink as="button" onClick={toggleTheme} style={{ width: "100%", border: "none", background: "none", cursor: "pointer" }}>
          {isDarkMode ? <FiSun /> : <FiMoon />}
          {isDarkMode ? t("nav.lightMode") : t("nav.darkMode")}
        </MobileNavLink>
        <MobileNavLink as="button" onClick={handleLogout} style={{ width: "100%", border: "none", background: "none", cursor: "pointer", color: "#ef4444" }}>
          <FiLogOut /> {t("nav.logout")}
        </MobileNavLink>
      </MobileMenu>

      <SearchModal isOpen={showSearch} onClose={() => setShowSearch(false)} />
    </>
  );
};

export default Navbar;
