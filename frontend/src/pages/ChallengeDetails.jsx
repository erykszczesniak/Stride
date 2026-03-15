import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
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
import { getIconForType, formatUnit, getDaysLeft, getAvatarColor } from "../utils/challengeHelpers";
import {
  BackLink,
  HeaderCard,
  CoverImage,
  ChallengeIcon,
  HeaderContent,
  BadgeRow,
  TypeBadge,
  Title,
  Description,
  ActionButtons,
  JoinButton,
  LeaveButton,
  DeleteButton,
  InfoCard,
  CardTitle,
  InfoGrid,
  InfoItem,
  InfoIconBox,
  InfoContent,
  InfoLabel,
  InfoValue,
  ProgressContainer,
  ProgressLabel,
  ProgressText,
  ProgressValue,
  ProgressBar,
  ProgressFill,
  LeaderboardContainer,
  LeaderboardHeader,
  LeaderboardTitle,
  LeaderboardSubtitle,
  LeaderboardList,
  LeaderboardItem,
  Rank,
  Avatar,
  UserName,
  UserScore,
  UserPercent,
  EmptyLeaderboard,
} from "./ChallengeDetailsStyles";

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
    }
  }, [id]);

  const fetchLeaderboard = useCallback(async () => {
    try {
      const res = await ChallengeService.getLeaderboard(id);
      setLeaderboard(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
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
