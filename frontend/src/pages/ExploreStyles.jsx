import styled from "styled-components";
import { Link } from "react-router-dom";

export const ExploreContainer = styled.div`
  max-width: 1200px;
  margin: 0 auto;
`;

export const SearchSection = styled.div`
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
`;

export const SearchWrapper = styled.div`
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
`;

export const SearchIcon = styled.div`
  position: absolute;
  left: 16px;
  color: #999;
  display: flex;
  align-items: center;
`;

export const SearchInput = styled.input`
  width: 100%;
  padding: 12px 16px 12px 48px;
  border: 1px solid #e0e0e0;
  border-radius: 24px;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: #999;
  }
`;

export const FilterButton = styled.button`
  width: 48px;
  height: 48px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;

  &:hover {
    background-color: #f5f5f5;
    border-color: #ccc;
  }
`;

export const TabsContainer = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  padding: 4px;
  background: #f5f5f5;
  border-radius: 12px;
  width: fit-content;
`;

export const Tab = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: ${(props) => (props.active ? "white" : "transparent")};
  color: ${(props) => (props.active ? "#333" : "#666")};
  box-shadow: ${(props) => (props.active ? "0 1px 3px rgba(0, 0, 0, 0.1)" : "none")};

  &:hover {
    color: #333;
  }
`;

export const TabIcon = styled.span`
  display: flex;
  align-items: center;
`;

export const MainContent = styled.div`
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
`;

export const FeedColumn = styled.div`
  display: flex;
  flex-direction: column;
`;

export const SidebarColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;

  @media (max-width: 1024px) {
    order: -1;
  }
`;

export const ActivityFeed = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const ActivityCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

export const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
`;

export const UserAvatar = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff7043 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
  text-decoration: none;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const UserInfo = styled.div`
  flex: 1;
`;

export const UserName = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-decoration: none;
  display: block;

  &:hover {
    color: #fc4c02;
  }
`;

export const ActivityTime = styled.p`
  font-size: 12px;
  color: #888;
  margin-top: 2px;
`;

export const ActivityTypeBadge = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background-color: ${(props) => props.bgColor || "#f0f0f0"};
  color: ${(props) => props.color || "#666"};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
`;

export const ActivityTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${(props) => props.titleColor || "#333"};
  margin-bottom: 8px;
  text-decoration: none;
  display: block;

  &:hover {
    text-decoration: underline;
  }
`;

export const ActivityDescription = styled.p`
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
  line-height: 1.5;
`;

export const StatsRow = styled.div`
  display: flex;
  gap: 32px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
`;

export const StatItem = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
`;

export const StatIcon = styled.span`
  font-size: 12px;
  opacity: 0.7;
`;

export const StatValue = styled.span`
  font-size: 18px;
  font-weight: 700;
  color: #333;
`;

export const StatLabel = styled.span`
  font-size: 11px;
  color: #888;
  text-transform: lowercase;
`;

export const CardFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const ActionsGroup = styled.div`
  display: flex;
  gap: 8px;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: ${(props) => (props.active ? "#fc4c02" : "#666")};
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  transition: background-color 0.2s, color 0.2s;

  &:hover {
    background-color: #f5f5f5;
    color: ${(props) => (props.active ? "#fc4c02" : "#333")};
  }

  svg {
    fill: ${(props) => (props.active ? "#fc4c02" : "none")};
  }
`;

export const SidebarCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const SidebarTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
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

export const AthleteAvatar = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff7043 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  text-decoration: none;
  cursor: pointer;

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

export const AthleteName = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: #333;
  text-decoration: none;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    color: #fc4c02;
  }
`;

export const AthleteStats = styled.p`
  font-size: 12px;
  color: #888;
  margin-top: 2px;
`;

export const FollowButton = styled.button`
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }
`;

export const SeeMoreLink = styled(Link)`
  display: block;
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 500;
  color: #fc4c02;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
`;

export const ChallengeCard = styled.div`
  background: linear-gradient(135deg, #fc4c02 0%, #ff7043 100%);
  border-radius: 12px;
  padding: 24px;
  color: white;
`;

export const ChallengeIcon = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 12px;
`;

export const ChallengeTitle = styled.h3`
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 8px;
`;

export const ChallengeDescription = styled.p`
  font-size: 14px;
  opacity: 0.9;
  line-height: 1.5;
  margin-bottom: 20px;
`;

export const ProgressSection = styled.div`
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
  font-size: 14px;
  font-weight: 600;
`;

export const ProgressBar = styled.div`
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
`;

export const ProgressFill = styled.div`
  width: ${(props) => props.progress || 0}%;
  height: 100%;
  background: white;
  border-radius: 4px;
  transition: width 0.3s ease;
`;

export const ParticipantsCount = styled.p`
  font-size: 12px;
  opacity: 0.8;
`;

export const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px;
  min-height: 400px;

  &::after {
    content: "";
    width: 40px;
    height: 40px;
    border: 3px solid #f0f0f0;
    border-top-color: #fc4c02;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;

export const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

export const EmptyText = styled.p`
  font-size: 14px;
  color: #666;
`;
