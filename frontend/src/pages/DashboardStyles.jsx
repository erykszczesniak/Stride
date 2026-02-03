import styled from "styled-components";
import { Link } from "react-router-dom";

export const DashboardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
`;

export const WelcomeSection = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
`;

export const WelcomeAvatar = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
`;

export const WelcomeText = styled.div`
  h1 {
    font-size: 26px;
    font-weight: 700;
    color: ${({ theme }) => theme.textPrimary};
    margin: 0 0 4px 0;
    display: flex;
    align-items: center;
    gap: 8px;
    transition: color 0.3s ease;
  }

  p {
    font-size: 14px;
    color: ${({ theme }) => theme.textSecondary};
    margin: 0;
    transition: color 0.3s ease;
  }
`;

export const UserAvatar = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover {
    transform: scale(1.05);
  }
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

export const DashboardContainer = styled.div`
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 28px;

  @media (max-width: 1200px) {
    grid-template-columns: 1fr;
  }
`;

export const MainColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const SideColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const Section = styled.section`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: background-color 0.3s ease, border-color 0.3s ease;
`;

export const SectionHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
`;

export const SectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const ViewAllLink = styled(Link)`
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: ${({ theme }) => theme.primary};
  text-decoration: none;
  font-weight: 500;
  transition: gap 0.2s;

  &:hover {
    gap: 8px;
  }

  &::after {
    content: "›";
    font-size: 16px;
  }
`;

export const ActivitiesList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const EmptyState = styled.div`
  text-align: center;
  padding: 48px 24px;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  transition: background-color 0.3s ease;
`;

export const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

export const EmptyText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 20px;
  transition: color 0.3s ease;
`;

export const AddButton = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  color: white;
  border-radius: 10px;
  text-decoration: none;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(252, 76, 2, 0.3);
  }
`;

export const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px;

  &::after {
    content: "";
    width: 40px;
    height: 40px;
    border: 3px solid ${({ theme }) => theme.border};
    border-top-color: ${({ theme }) => theme.primary};
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;
