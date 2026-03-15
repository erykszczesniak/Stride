import styled from "styled-components";
import { Link } from "react-router-dom";

export const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const SearchContainer = styled.div`
  position: relative;
  margin-bottom: 20px;
`;

export const SearchInput = styled.input`
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  font-size: 15px;
  background-color: ${({ theme }) => theme.bgSecondary};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
    background-color: ${({ theme }) => theme.bgCard};
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
  }
`;

export const SearchIcon = styled.div`
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: ${({ theme }) => theme.textMuted};
`;

export const MainTabs = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
  padding-bottom: 12px;
  transition: border-color 0.3s ease;
`;

export const MainTab = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? props.theme.primary : "transparent")};
  color: ${(props) => (props.active ? "white" : props.theme.textSecondary)};

  &:hover {
    background-color: ${(props) => (props.active ? "#e04402" : props.theme.bgHover)};
  }

  svg {
    width: 18px;
    height: 18px;
  }
`;

export const FilterTabs = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
`;

export const FilterTab = styled.button`
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? props.theme.primaryLight : props.theme.bgHover)};
  color: ${(props) => (props.active ? props.theme.primary : props.theme.textSecondary)};
  border: 1px solid ${(props) => (props.active ? props.theme.primary : "transparent")};

  &:hover {
    background-color: ${(props) => (props.active ? props.theme.primaryLight : props.theme.bgSecondary)};
  }
`;

export const ContentWrapper = styled.div`
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
`;

export const FeedColumn = styled.div``;

export const SidebarColumn = styled.div`
  @media (max-width: 1024px) {
    display: none;
  }
`;

export const SidebarCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  margin-bottom: 20px;
  transition: all 0.3s ease;
`;

export const SidebarTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 16px;
  transition: color 0.3s ease;
`;

export const AthletesList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const AthleteItem = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const AthleteAvatar = styled(Link)`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const AthleteInfo = styled.div`
  flex: 1;
  min-width: 0;
`;

export const AthleteName = styled(Link)`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  text-decoration: none;
  display: block;
  transition: color 0.3s ease;

  &:hover {
    color: ${({ theme }) => theme.primary};
  }
`;

export const AthleteStats = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textMuted};
  transition: color 0.3s ease;
`;

export const FollowButton = styled.button`
  padding: 6px 14px;
  background: #fc4c02;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #e04402;
  }
`;

export const ChallengeCard = styled(Link)`
  display: block;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 16px;
  padding: 20px;
  color: white;
  text-decoration: none;
  margin-bottom: 20px;
`;

export const ChallengeHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
`;

export const ChallengeIcon = styled.span`
  font-size: 20px;
`;

export const ChallengeTitle = styled.h4`
  font-size: 16px;
  font-weight: 700;
  margin: 0;
`;

export const ChallengeDescription = styled.p`
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 16px 0;
  line-height: 1.4;
`;

export const ProgressBox = styled.div`
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 12px;
`;

export const ProgressHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

export const ProgressLabel = styled.span`
  font-size: 13px;
  opacity: 0.9;
`;

export const ProgressValue = styled.span`
  font-size: 15px;
  font-weight: 700;
`;

export const ProgressBar = styled.div`
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  overflow: hidden;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background: white;
  border-radius: 3px;
  width: ${(props) => Math.min(props.progress, 100)}%;
  transition: width 0.3s ease;
`;

export const ParticipantsCount = styled.p`
  font-size: 13px;
  opacity: 0.8;
  margin: 0;
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

export const StatCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const StatIconWrapper = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background-color: ${(props) => props.color || "#fc4c02"}15;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${(props) => props.color || "#fc4c02"};
`;

export const StatContent = styled.div`
  flex: 1;
`;

export const StatValue = styled.div`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const StatLabel = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textMuted};
  margin-top: 2px;
  transition: color 0.3s ease;
`;
