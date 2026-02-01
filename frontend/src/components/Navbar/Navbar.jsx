import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { FiPlus, FiBell, FiSearch, FiSettings } from "react-icons/fi";
import { useAuth } from "../../context/AuthContext";
import {
  Nav,
  Logo,
  LogoIcon,
  NavLinks,
  NavLink,
  NavRight,
  IconButton,
  Avatar,
  AvatarWrapper,
  DropdownMenu,
  DropdownItem,
  DropdownButton,
} from "./NavbarStyles";

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);

  const isActive = (path) => location.pathname === path;

  const handleLogout = () => {
    logout();
    navigate("/login");
    setShowDropdown(false);
  };

  const getInitials = () => {
    if (!user) return "??";
    const first = user.firstName?.[0] || "";
    const last = user.lastName?.[0] || "";
    return (first + last).toUpperCase() || "??";
  };

  return (
    <Nav>
      <Logo to="/">
        <LogoIcon>🏃</LogoIcon>
        Strava Clone
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
        <IconButton title="Search">
          <FiSearch size={20} />
        </IconButton>
        <IconButton title="Add Activity" onClick={() => navigate("/activities/new")}>
          <FiPlus size={22} />
        </IconButton>
        <IconButton title="Notifications">
          <FiBell size={20} />
        </IconButton>
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
      </NavRight>
    </Nav>
  );
};

export default Navbar;
