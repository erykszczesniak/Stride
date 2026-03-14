import React, { useEffect, useState, useCallback } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import {
  FiArrowLeft,
  FiUsers,
  FiCalendar,
  FiAward,
  FiTarget,
  FiTrash2,
  FiClock,
  FiTrendingUp,
  FiEye,
} from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ChallengeService from "../api/ChallengeService";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { LoadingSpinner } from "./DashboardStyles";
import styled from "styled-components";

const BackLink = styled(Link)`
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

const HeaderCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const CoverImage = styled.div`
  height: 200px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
`;

const ChallengeIcon = styled.span`
  font-size: 64px;
`;

const HeaderContent = styled.div`
  padding: 24px;
`;

const BadgeRow = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
`;

const TypeBadge = styled.span`
  display: inline-block;
  background: ${(props) => props.color || "#fc4c02"};
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
`;

const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 16px 0;
`;

const ActionButtons = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 20px;
`;

const JoinButton = styled.button`
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

const LeaveButton = styled.button`
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

const DeleteButton = styled.button`
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

const InfoCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
`;

const InfoItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

const InfoIconBox = styled.div`
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

const InfoContent = styled.div``;

const InfoLabel = styled.p`
  font-size: 12px;
  color: #999;
  margin: 0 0 4px 0;
`;

const InfoValue = styled.p`
  font-size: 15px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

const ProgressContainer = styled.div`
  margin-top: 8px;
`;

const ProgressLabel = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

const ProgressText = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
`;

const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

const ProgressBar = styled.div`
  height: 10px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 5px;
  overflow: hidden;
`;

const ProgressFill = styled.div`
  height: 100%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  width: ${(props) => Math.min(props.percentage, 100)}%;
  border-radius: 5px;
  transition: width 0.3s ease;
`;

const LeaderboardContainer = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
`;

const LeaderboardHeader = styled.div`
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  padding: 20px 24px;
  color: white;
`;

const LeaderboardTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 4px 0;
`;

const LeaderboardSubtitle = styled.p`
  font-size: 14px;
  opacity: 0.9;
  margin: 0;
`;

const LeaderboardList = styled.div``;

const LeaderboardItem = styled.div`
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

const Rank = styled.div`
  width: 32px;
  font-size: 14px;
  font-weight: 500;
  color: ${(props) =>
    props.isTop3 ? props.theme.primary : props.theme.textSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
`;

const Avatar = styled.div`
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

const UserName = styled.span`
  flex: 1;
  font-size: 15px;
  font-weight: ${(props) => (props.isCurrentUser ? "600" : "400")};
  color: ${(props) =>
    props.isCurrentUser ? props.theme.primary : props.theme.textPrimary};
`;

const UserScore = styled.span`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-right: 12px;
`;

const UserPercent = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  min-width: 45px;
  text-align: right;
`;

const EmptyLeaderboard = styled.div`
  padding: 40px 24px;
  text-align: center;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 14px;
`;

const activityTypeConfig = {
  RUN: { icon: "🏃", label: "Run", color: "#fc5200" },
  RIDE: { icon: "🚴", label: "Cycling", color: "#0095ff" },
  SWIM: { icon: "🏊", label: "Swimming", color: "#00bcd4" },
  HIKE: { icon: "⛰️", label: "Hiking", color: "#4caf50" },
  WALK: { icon: "🚶", label: "Walking", color: "#9c27b0" },
  ANY: { icon: "🏆", label: "Any Activity", color: "#ff9800" },
};

const challengeTypeConfig = {
  DISTANCE: { label: "Distance", color: "#2196f3" },
  TIME: { label: "Time", color: "#9c27b0" },
  ELEVATION: { label: "Elevation", color: "#4caf50" },
  ACTIVITIES_COUNT: { label: "Activity Count", color: "#ff9800" },
};

const getIconForType = (challengeType, activityType) => {
  if (activityType === "RUN") return "🏃";
  if (activityType === "RIDE") return "🚴";
  if (activityType === "SWIM") return "🏊";
  if (challengeType === "DISTANCE") return "📍";
  if (challengeType === "TIME") return "⏱️";
  if (challengeType === "ELEVATION") return "⛰️";
  if (challengeType === "ACTIVITIES_COUNT") return "📅";
  return "🏆";
};

const formatUnit = (challengeType) => {
  switch (challengeType) {
    case "DISTANCE":
      return "km";
    case "TIME":
      return "hours";
    case "ELEVATION":
      return "m";
    case "ACTIVITIES_COUNT":
      return "activities";
    default:
      return "";
  }
};

const getDaysLeft = (endDate) => {
  const end = new Date(endDate);
  const now = new Date();
  const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24));
  return diff > 0 ? diff : 0;
};

const getAvatarColor = (index) => {
  const colors = ["#5c6bc0", "#26a69a", "#ef5350", "#7e57c2", "#42a5f5"];
  return colors[index % colors.length];
};

const ChallengeDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const toast = useToast();
  const [challenge, setChallenge] = useState(null);
  const [leaderboard, setLeaderboard] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchChallenge = useCallback(async () => {
    try {
      const res = await ChallengeService.getChallengeById(id);
      setChallenge(res.data);
    } catch (error) {
      // TODO: Add error reporting service
    }
  }, [id]);

  const fetchLeaderboard = useCallback(async () => {
    try {
      const res = await ChallengeService.getLeaderboard(id);
      setLeaderboard(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      // TODO: Add error reporting service
    }
  }, [id]);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      await Promise.all([fetchChallenge(), fetchLeaderboard()]);
      setLoading(false);
    };
    fetchData();
  }, [fetchChallenge, fetchLeaderboard]);

  const handleJoin = async () => {
    setActionLoading(true);
    try {
      await ChallengeService.joinChallenge(id);
      await Promise.all([fetchChallenge(), fetchLeaderboard()]);
      toast.success("Successfully joined the challenge!");
    } catch (error) {
      // TODO: Add error reporting service
      toast.error("Failed to join the challenge.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleLeave = async () => {
    setActionLoading(true);
    try {
      await ChallengeService.leaveChallenge(id);
      await Promise.all([fetchChallenge(), fetchLeaderboard()]);
      toast.success("You left the challenge.");
    } catch (error) {
      // TODO: Add error reporting service
      toast.error("Failed to leave the challenge.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this challenge?"))
      return;
    try {
      await ChallengeService.deleteChallenge(id);
      toast.success("Challenge deleted.");
      navigate("/challenges");
    } catch (error) {
      // TODO: Add error reporting service
      toast.error("Failed to delete the challenge.");
    }
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  if (!challenge) {
    return (
      <Layout>
        <BackLink to="/challenges">
          <FiArrowLeft /> Back to Challenges
        </BackLink>
        <InfoCard>
          <p>Challenge not found.</p>
        </InfoCard>
      </Layout>
    );
  }

  const isCreator = challenge.creatorId === user?.id;
  const isParticipating = challenge.participating;
  const daysLeft = getDaysLeft(challenge.endDate);
  const isEnded = daysLeft === 0;
  const unit = formatUnit(challenge.challengeType);
  const icon = getIconForType(challenge.challengeType, challenge.activityType);
  const activityConfig = activityTypeConfig[challenge.activityType] ||
    activityTypeConfig.ANY;
  const typeConfig = challengeTypeConfig[challenge.challengeType] || {
    label: challenge.challengeType,
    color: "#fc4c02",
  };

  const progress = challenge.userProgress || 0;
  const percentage = challenge.targetValue
    ? (progress / challenge.targetValue) * 100
    : 0;

  return (
    <Layout>
      <BackLink to="/challenges">
        <FiArrowLeft /> Back to Challenges
      </BackLink>

      <HeaderCard>
        <CoverImage>
          <ChallengeIcon>{icon}</ChallengeIcon>
        </CoverImage>
        <HeaderContent>
          <BadgeRow>
            <TypeBadge color={typeConfig.color}>{typeConfig.label}</TypeBadge>
            <TypeBadge color={activityConfig.color}>
              {activityConfig.label}
            </TypeBadge>
          </BadgeRow>
          <Title>{challenge.name}</Title>

          {challenge.description && (
            <Description>{challenge.description}</Description>
          )}

          <ActionButtons>
            {isParticipating ? (
              !isCreator && (
                <LeaveButton onClick={handleLeave} disabled={actionLoading}>
                  {actionLoading
                    ? "Processing..."
                    : "✓ Joined - Click to Leave"}
                </LeaveButton>
              )
            ) : (
              <JoinButton
                onClick={handleJoin}
                disabled={actionLoading || isEnded}
              >
                {actionLoading
                  ? "Processing..."
                  : isEnded
                    ? "Challenge Ended"
                    : "Join Challenge"}
              </JoinButton>
            )}
            {isCreator && (
              <DeleteButton onClick={handleDelete}>
                <FiTrash2 size={16} />
              </DeleteButton>
            )}
          </ActionButtons>
        </HeaderContent>
      </HeaderCard>

      <InfoCard>
        <CardTitle>
          <FiAward /> Challenge Details
        </CardTitle>
        <InfoGrid>
          <InfoItem>
            <InfoIconBox>
              <FiTarget size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>Target</InfoLabel>
              <InfoValue>
                {challenge.targetValue} {unit}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiCalendar size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>Start Date</InfoLabel>
              <InfoValue>
                {new Date(challenge.startDate).toLocaleDateString("en-US", {
                  month: "long",
                  day: "numeric",
                  year: "numeric",
                })}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiCalendar size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>End Date</InfoLabel>
              <InfoValue>
                {new Date(challenge.endDate).toLocaleDateString("en-US", {
                  month: "long",
                  day: "numeric",
                  year: "numeric",
                })}
              </InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiClock size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>Days Left</InfoLabel>
              <InfoValue>{isEnded ? "Ended" : `${daysLeft} days`}</InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiUsers size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>Participants</InfoLabel>
              <InfoValue>{challenge.participantsCount || 0}</InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiEye size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>Visibility</InfoLabel>
              <InfoValue>
                {challenge.visibility === "PRIVATE" ? "Private" : "Public"}
              </InfoValue>
            </InfoContent>
          </InfoItem>
        </InfoGrid>
      </InfoCard>

      {isParticipating && (
        <InfoCard>
          <CardTitle>
            <FiTrendingUp /> Your Progress
          </CardTitle>
          <ProgressContainer>
            <ProgressLabel>
              <ProgressText>
                {percentage >= 100 ? "Completed!" : "In Progress"}
              </ProgressText>
              <ProgressValue>
                {progress.toFixed(1)} / {challenge.targetValue} {unit}
              </ProgressValue>
            </ProgressLabel>
            <ProgressBar>
              <ProgressFill percentage={percentage} />
            </ProgressBar>
          </ProgressContainer>
        </InfoCard>
      )}

      <LeaderboardContainer>
        <LeaderboardHeader>
          <LeaderboardTitle>Leaderboard</LeaderboardTitle>
          <LeaderboardSubtitle>
            {leaderboard.length} participant
            {leaderboard.length !== 1 ? "s" : ""}
          </LeaderboardSubtitle>
        </LeaderboardHeader>
        <LeaderboardList>
          {leaderboard.length === 0 ? (
            <EmptyLeaderboard>No participants yet</EmptyLeaderboard>
          ) : (
            leaderboard.map((entry, index) => {
              const isCurrentUser = entry.isCurrentUser;
              const isTop3 = index < 3;
              const entryPercent = challenge.targetValue
                ? ((entry.progress || 0) / challenge.targetValue) * 100
                : 0;
              return (
                <LeaderboardItem key={entry.userId} isCurrentUser={isCurrentUser}>
                  <Rank isTop3={isTop3}>
                    {isTop3 ? ["🥇", "🥈", "🥉"][index] : index + 1}
                  </Rank>
                  <Avatar bgColor={getAvatarColor(index)}>
                    {entry.userName?.charAt(0) || "?"}
                  </Avatar>
                  <UserName isCurrentUser={isCurrentUser}>
                    {isCurrentUser ? "You" : entry.userName}
                  </UserName>
                  <UserScore>
                    {(entry.progress || 0).toFixed(1)} {unit}
                  </UserScore>
                  <UserPercent>{Math.min(entryPercent, 100).toFixed(0)}%</UserPercent>
                </LeaderboardItem>
              );
            })
          )}
        </LeaderboardList>
      </LeaderboardContainer>
    </Layout>
  );
};

export default ChallengeDetails;
