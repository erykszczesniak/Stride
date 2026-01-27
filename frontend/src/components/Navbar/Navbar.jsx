import React, { useState } from "react";
import { useLocation } from "react-router-dom";
import { FiPlus, FiBell, FiSearch, FiSettings } from "react-icons/fi";
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
} from "./NavbarStyles";

const Navbar = () => {
  const location = useLocation();
  const [showDropdown, setShowDropdown] = useState(false);

  const isActive = (path) => location.pathname === path;

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
        <NavLink to="/explore" className={isActive("/explore") ? "active" : ""}>
          Explore
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
        <IconButton title="Add Activity">
          <FiPlus size={22} />
        </IconButton>
        <IconButton title="Notifications">
          <FiBell size={20} />
        </IconButton>
        <AvatarWrapper>
          <Avatar onClick={() => setShowDropdown(!showDropdown)}>
            JD
          </Avatar>
          {showDropdown && (
            <DropdownMenu>
              <DropdownItem to="/profile">My Profile</DropdownItem>
              <DropdownItem to="/goals">Goals</DropdownItem>
              <DropdownItem to="/training">Training Plans</DropdownItem>
              <DropdownItem to="/admin">Admin Panel</DropdownItem>
              <DropdownItem to="/settings">Settings</DropdownItem>
              <DropdownItem to="/logout">Log Out</DropdownItem>
            </DropdownMenu>
          )}
        </AvatarWrapper>
      </NavRight>
    </Nav>
  );
};

export default Navbar;
