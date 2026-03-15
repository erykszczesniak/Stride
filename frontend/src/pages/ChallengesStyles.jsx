import styled from "styled-components";

export const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
`;

export const HeaderLeft = styled.div``;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const PageSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 4px 0 0 0;
  transition: color 0.3s ease;
`;

export const HeaderRight = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const CreateButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
  }
`;

export const ActiveBadge = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #fff3ed;
  color: #fc4c02;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 14px;
`;

export const TabsContainer = styled.div`
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background-color: ${({ theme }) => theme.bgHover};
  padding: 4px;
  border-radius: 10px;
  width: fit-content;
  transition: background-color 0.3s ease;
`;

export const Tab = styled.button`
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? props.theme.bgCard : "transparent")};
  color: ${(props) => (props.active ? props.theme.textPrimary : props.theme.textSecondary)};
  box-shadow: ${(props) => (props.active ? `0 1px 3px ${props.theme.shadow}` : "none")};
  position: relative;

  &:hover {
    color: ${({ theme }) => theme.textPrimary};
  }
`;

export const TabBadge = styled.span`
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background-color: #fc4c02;
  border-radius: 50%;
`;

export const ChallengeCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  display: flex;
  align-items: flex-start;
  gap: 16px;
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    box-shadow: 0 4px 12px ${({ theme }) => theme.shadow};
    transform: translateY(-1px);
  }
`;

export const ChallengeIcon = styled.div`
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  background-color: ${(props) => props.bgColor || "#fff3ed"};
  color: ${(props) => props.iconColor || "#fc4c02"};
`;

export const ChallengeContent = styled.div`
  flex: 1;
  min-width: 0;
`;

export const ChallengeTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 4px 0;
  transition: color 0.3s ease;
`;

export const ChallengeDescription = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 12px 0;
  transition: color 0.3s ease;
`;

export const ChallengeMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
`;

export const MetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

export const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background-color: ${({ theme }) => theme.bgHover};
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.3s ease;
`;

export const ProgressContainer = styled.div`
  margin-top: 16px;
`;

export const ProgressLabel = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

export const ProgressText = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

export const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const ProgressBar = styled.div`
  height: 8px;
  background-color: ${({ theme }) => theme.bgHover};
  border-radius: 4px;
  overflow: hidden;
  transition: background-color 0.3s ease;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background-color: #fc4c02;
  border-radius: 4px;
  width: ${(props) => Math.min(props.percentage, 100)}%;
  transition: width 0.3s ease;
`;

export const JoinButton = styled.button`
  background-color: #fc4c02;
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.2s;

  &:hover {
    background-color: #e04402;
  }

  &:disabled {
    background-color: #ccc;
    cursor: not-allowed;
  }
`;

export const LeaveButton = styled.button`
  background-color: transparent;
  color: ${({ theme }) => theme.textSecondary};
  border: 1px solid ${({ theme }) => theme.border};
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  align-self: flex-start;
  transition: all 0.2s;

  &:hover {
    color: #e53935;
    border-color: #e53935;
    background-color: #ffebee;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const LeaderboardContainer = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const LeaderboardHeader = styled.div`
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  padding: 20px 24px;
  color: white;
`;

export const LeaderboardTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
`;

export const LeaderboardSubtitle = styled.p`
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
`;

export const LeaderboardList = styled.div``;

export const LeaderboardItem = styled.div`
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
  background-color: ${(props) => (props.isCurrentUser ? props.theme.primaryLight : props.theme.bgCard)};
  transition: all 0.3s ease;

  &:last-child {
    border-bottom: none;
  }
`;

export const Rank = styled.div`
  width: 32px;
  font-size: 14px;
  font-weight: 500;
  color: ${(props) => (props.isTop3 ? props.theme.primary : props.theme.textSecondary)};
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.3s ease;
`;

export const Avatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: ${(props) => props.bgColor || "#e0e0e0"};
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  color: white;
  margin-right: 12px;
`;

export const UserName = styled.span`
  flex: 1;
  font-size: 15px;
  font-weight: ${(props) => (props.isCurrentUser ? "600" : "400")};
  color: ${(props) => (props.isCurrentUser ? props.theme.primary : props.theme.textPrimary)};
  transition: color 0.3s ease;
`;

export const UserScore = styled.span`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const EmptyState = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

export const EmptyText = styled.p`
  font-size: 16px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  padding: 60px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const ChallengeSelect = styled.select`
  padding: 10px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  cursor: pointer;
  transition: all 0.3s ease;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
  }
`;
