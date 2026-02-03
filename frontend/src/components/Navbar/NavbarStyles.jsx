import styled from "styled-components";
import { Link } from "react-router-dom";
import { media } from "../../styles/theme";

export const Nav = styled.nav`
  background-color: #fc4c02;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 1000;

  ${media.md} {
    padding: 0 16px;
  }
`;

export const Logo = styled(Link)`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 700;
  color: white;
  text-decoration: none;

  ${media.sm} {
    font-size: 18px;
    gap: 6px;
  }
`;

export const LogoIcon = styled.span`
  font-size: 28px;

  ${media.sm} {
    font-size: 24px;
  }
`;

export const LogoText = styled.span`
  ${media.xs} {
    display: none;
  }
`;

export const NavLinks = styled.div`
  display: flex;
  align-items: center;
  gap: 32px;

  ${media.lg} {
    display: none;
  }
`;

export const NavLink = styled(Link)`
  color: white;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }

  &.active {
    background-color: rgba(255, 255, 255, 0.2);
  }
`;

export const NavRight = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;

  ${media.md} {
    gap: 8px;
  }
`;

export const IconButton = styled.button`
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
  min-width: 40px;
  min-height: 40px;

  &:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }

  ${media.sm} {
    padding: 6px;
    min-width: 36px;
    min-height: 36px;
  }
`;

export const HideOnMobile = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  ${media.md} {
    display: none;
  }
`;

export const MobileMenuButton = styled(IconButton)`
  display: none;

  ${media.lg} {
    display: flex;
  }
`;

export const Avatar = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #fc4c02;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const DropdownMenu = styled.div`
  position: absolute;
  top: 100%;
  right: 0;
  background: ${({ theme }) => theme.dropdownBg};
  border-radius: 8px;
  box-shadow: 0 4px 12px ${({ theme }) => theme.shadowMedium};
  min-width: 200px;
  padding: 8px 0;
  margin-top: 8px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const DropdownItem = styled(Link)`
  display: block;
  padding: 12px 16px;
  color: ${({ theme }) => theme.textPrimary};
  text-decoration: none;
  font-size: 14px;
  transition: background-color 0.2s;

  &:hover {
    background-color: ${({ theme }) => theme.dropdownHover};
  }
`;

export const AvatarWrapper = styled.div`
  position: relative;
`;

export const DropdownButton = styled.button`
  display: block;
  width: 100%;
  padding: 12px 16px;
  color: ${({ theme }) => theme.error};
  text-decoration: none;
  font-size: 14px;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s;

  &:hover {
    background-color: ${({ theme }) => theme.dropdownHover};
  }
`;

// Mobile Menu Overlay
export const MobileMenuOverlay = styled.div`
  display: none;

  ${media.lg} {
    display: ${({ $isOpen }) => ($isOpen ? "block" : "none")};
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 998;
  }
`;

export const MobileMenu = styled.div`
  display: none;

  ${media.lg} {
    display: ${({ $isOpen }) => ($isOpen ? "flex" : "none")};
    position: fixed;
    top: 64px;
    left: 0;
    right: 0;
    background: ${({ theme }) => theme.bgSecondary};
    flex-direction: column;
    padding: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    z-index: 999;
    max-height: calc(100vh - 64px);
    overflow-y: auto;
  }
`;

export const MobileNavLink = styled(Link)`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  color: ${({ theme }) => theme.textPrimary};
  text-decoration: none;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: background-color 0.2s;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
  }

  &.active {
    background-color: ${({ theme }) => theme.primaryLight};
    color: ${({ theme }) => theme.primary};
  }

  svg {
    width: 20px;
    height: 20px;
  }
`;

export const MobileDivider = styled.div`
  height: 1px;
  background: ${({ theme }) => theme.border};
  margin: 8px 0;
`;
