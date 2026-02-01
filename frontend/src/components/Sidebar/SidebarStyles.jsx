import styled from "styled-components";
import { Link } from "react-router-dom";

export const SidebarContainer = styled.aside`
  width: 260px;
  height: 100vh;
  background: white;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  padding: 24px 16px;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 100;

  @media (max-width: 1024px) {
    width: 80px;
    padding: 24px 12px;
  }
`;

export const Logo = styled(Link)`
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  margin-bottom: 32px;
  padding: 0 8px;
`;

export const LogoIcon = styled.div`
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;

  @media (max-width: 1024px) {
    width: 48px;
    height: 48px;
  }
`;

export const LogoText = styled.span`
  font-size: 20px;
  font-weight: 800;
  color: #1a1a2e;
  letter-spacing: -0.5px;

  @media (max-width: 1024px) {
    display: none;
  }
`;

export const RecordButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 20px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  margin-bottom: 32px;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(252, 76, 2, 0.3);
  }

  @media (max-width: 1024px) {
    padding: 14px;

    span {
      display: none;
    }
  }
`;

export const NavSection = styled.nav`
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
`;

export const NavItem = styled(Link)`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  text-decoration: none;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background: #f8fafc;
    color: #1a1a2e;
  }

  &.active {
    color: #fc4c02;
    background: #fff7ed;

    svg {
      color: #fc4c02;
    }
  }

  @media (max-width: 1024px) {
    justify-content: center;
    padding: 12px;
  }
`;

export const NavIcon = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
`;

export const NavLabel = styled.span`
  @media (max-width: 1024px) {
    display: none;
  }
`;

export const ActiveDot = styled.span`
  width: 6px;
  height: 6px;
  background: #fc4c02;
  border-radius: 50%;
  margin-left: auto;

  @media (max-width: 1024px) {
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
  }
`;

export const BottomSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  margin-top: auto;
`;

export const LogoutButton = styled.button`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  text-decoration: none;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  background: none;
  border: none;
  cursor: pointer;
  width: 100%;
  text-align: left;

  &:hover {
    background: #fef2f2;
    color: #dc2626;

    svg {
      color: #dc2626;
    }
  }

  @media (max-width: 1024px) {
    justify-content: center;
    padding: 12px;
  }
`;
