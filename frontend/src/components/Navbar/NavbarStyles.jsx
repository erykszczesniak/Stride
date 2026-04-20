import styled from "styled-components";
import { Link } from "react-router-dom";
import { media } from "../../styles/theme";

export const Nav = styled.nav`
  background: linear-gradient(135deg, #fc4c02 0%, #e04400 100%);
  border-bottom: none;
  display: flex;
  align-items: center;
  padding: 0 24px;
  height: 64px;
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: background-color 0.3s ease, border-color 0.3s ease;

  ${media.md} {
    padding: 0 16px;
  }
`;

export const Logo = styled(Link)`
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 800;
  color: white;
  text-decoration: none;
  letter-spacing: -0.5px;

  ${media.sm} {
    font-size: 18px;
    gap: 8px;
  }
`;

export const LogoIcon = styled.div`
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;

  ${media.sm} {
    width: 32px;
    height: 32px;
  }
`;

export const LogoText = styled.span`
  ${media.xs} {
    display: none;
  }
`;

export const NavLinks = styled.div`
  display: flex;
  align-items: stretch;
  gap: 4px;
  margin: 0 auto;
  height: 100%;

  ${media.lg} {
    display: none;
  }
`;

export const NavLink = styled(Link)`
  color: rgba(255, 255, 255, 0.95);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  padding: 0 12px;
  display: flex;
  align-items: center;
  border-radius: 0;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    color: white;
  }

  &.active {
    color: white;
    border-bottom-color: white;
    font-weight: 600;
  }
`;

export const NavRight = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;

  /* Once NavLinks hides at <=1024px, there's no centred element stretching
     the row, so NavRight would otherwise sit right next to the logo.
     margin-left: auto pushes it back to the right edge. */
  ${media.lg} {
    margin-left: auto;
  }

  ${media.md} {
    gap: 10px;
  }
`;

export const IconButton = styled.button`
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  min-width: 40px;
  min-height: 40px;

  &:hover {
    color: white;
    background-color: rgba(255, 255, 255, 0.15);
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
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 13px;
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
