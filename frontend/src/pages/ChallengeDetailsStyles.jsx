import styled from "styled-components";
import { Link } from "react-router-dom";

export const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;

  &:hover {
    color: #fc4c02;
  }
`;

export const HeaderCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const CoverImage = styled.div`
  height: 200px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const ChallengeIcon = styled.span`
  font-size: 64px;
`;

export const HeaderContent = styled.div`
  padding: 24px;
`;

export const BadgeRow = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
`;

export const TypeBadge = styled.span`
  display: inline-block;
  background: ${(props) => props.color || "#fc4c02"};
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
`;

export const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

export const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 16px 0;
`;

export const ActionButtons = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 20px;
`;

export const JoinButton = styled.button`
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  color: white;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
`;

export const LeaveButton = styled.button`
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  background: #e8f5e9;
  color: #2e7d32;

  &:hover {
    background: #ffebee;
    color: #c62828;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

export const DeleteButton = styled.button`
  padding: 14px 20px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;

  &:hover {
    border-color: #e53935;
    color: #e53935;
  }
`;

export const InfoCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
`;

export const InfoItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

export const InfoIconBox = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: ${({ theme }) => theme.bgSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.textSecondary};
  flex-shrink: 0;
`;

export const InfoContent = styled.div``;

export const InfoLabel = styled.p`
  font-size: 12px;
  color: #999;
  margin: 0 0 4px 0;
`;

export const InfoValue = styled.p`
  font-size: 15px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const ProgressContainer = styled.div`
  margin-top: 8px;
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
`;

export const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const ProgressBar = styled.div`
  height: 10px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 5px;
  overflow: hidden;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  width: ${(props) => Math.min(props.percentage, 100)}%;
  border-radius: 5px;
  transition: width 0.3s ease;
`;

export const LeaderboardContainer = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
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
  background-color: ${(props) =>
    props.isCurrentUser ? props.theme.primaryLight : props.theme.bgCard};
  transition: all 0.3s ease;

  &:last-child {
    border-bottom: none;
  }
`;

export const Rank = styled.div`
  width: 32px;
  font-size: 14px;
  font-weight: 500;
  color: ${(props) =>
    props.isTop3 ? props.theme.primary : props.theme.textSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
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
  color: ${(props) =>
    props.isCurrentUser ? props.theme.primary : props.theme.textPrimary};
`;

export const UserScore = styled.span`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-right: 12px;
`;

export const UserPercent = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  min-width: 45px;
  text-align: right;
`;

export const EmptyLeaderboard = styled.div`
  padding: 40px 24px;
  text-align: center;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 14px;
`;
