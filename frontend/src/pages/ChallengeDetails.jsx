import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import i18n from "../i18n";
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

const activityTypeColors = {
  RUN: "#fc5200",
  RIDE: "#0095ff",
  SWIM: "#00bcd4",
  HIKE: "#4caf50",
  WALK: "#9c27b0",
  ANY: "#ff9800",
};

const challengeTypeColors = {
  DISTANCE: "#2196f3",
  TIME: "#9c27b0",
  ELEVATION: "#4caf50",
  ACTIVITIES_COUNT: "#ff9800",
};

const ChallengeDetails = () => {
  const { t } = useTranslation();
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
      toast.success(t("challenges.toast.joinSuccess"));
    } catch (error) {
      toast.error(t("challenges.toast.joinFailed"));
    } finally {
      setActionLoading(false);
    }
  };

  const handleLeave = async () => {
    setActionLoading(true);
    try {
      await ChallengeService.leaveChallenge(id);
      await Promise.all([fetchChallenge(), fetchLeaderboard()]);
      toast.success(t("challenges.toast.leaveSuccess"));
    } catch (error) {
      toast.error(t("challenges.toast.leaveFailed"));
    } finally {
      setActionLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm(t("challenges.confirmDelete")))
      return;
    try {
      await ChallengeService.deleteChallenge(id);
      toast.success(t("challenges.toast.deleted"));
      navigate("/challenges");
    } catch (error) {
      toast.error(t("challenges.toast.deleteFailed"));
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
          <FiArrowLeft /> {t("challenges.backToChallenges")}
        </BackLink>
        <InfoCard>
          <p>{t("challenges.notFound")}</p>
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
  const activityColor = activityTypeColors[challenge.activityType] || activityTypeColors.ANY;
  const activityLabel = t(`activities.types.${challenge.activityType || "ANY"}`, { defaultValue: challenge.activityType || "" });
  const typeColor = challengeTypeColors[challenge.challengeType] || "#fc4c02";
  const typeLabel = t(`challenges.types.${challenge.challengeType}`, { defaultValue: challenge.challengeType });

  const progress = challenge.userProgress || 0;
  const percentage = challenge.targetValue
    ? (progress / challenge.targetValue) * 100
    : 0;

  return (
    <Layout>
      <BackLink to="/challenges">
        <FiArrowLeft /> {t("challenges.backToChallenges")}
      </BackLink>

      <HeaderCard>
        <CoverImage>
          <ChallengeIcon>{icon}</ChallengeIcon>
        </CoverImage>
        <HeaderContent>
          <BadgeRow>
            <TypeBadge color={typeColor}>{typeLabel}</TypeBadge>
            <TypeBadge color={activityColor}>
              {activityLabel}
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
                    ? t("challenges.processing")
                    : t("challenges.joinedClickToLeave")}
                </LeaveButton>
              )
            ) : (
              <JoinButton
                onClick={handleJoin}
                disabled={actionLoading || isEnded}
              >
                {actionLoading
                  ? t("challenges.processing")
                  : isEnded
                    ? t("challenges.challengeEnded")
                    : t("challenges.joinChallenge")}
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
          <FiAward /> {t("challenges.details")}
        </CardTitle>
        <InfoGrid>
          <InfoItem>
            <InfoIconBox>
              <FiTarget size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>{t("challenges.target")}</InfoLabel>
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
              <InfoLabel>{t("challenges.startDate")}</InfoLabel>
              <InfoValue>
                {new Date(challenge.startDate).toLocaleDateString(i18n.language, {
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
              <InfoLabel>{t("challenges.endDate")}</InfoLabel>
              <InfoValue>
                {new Date(challenge.endDate).toLocaleDateString(i18n.language, {
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
              <InfoLabel>{t("challenges.daysLeftLabel")}</InfoLabel>
              <InfoValue>{isEnded ? t("challenges.ended") : t("challenges.daysValue", { count: daysLeft })}</InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiUsers size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>{t("challenges.participants")}</InfoLabel>
              <InfoValue>{challenge.participantsCount || 0}</InfoValue>
            </InfoContent>
          </InfoItem>

          <InfoItem>
            <InfoIconBox>
              <FiEye size={18} />
            </InfoIconBox>
            <InfoContent>
              <InfoLabel>{t("challenges.visibility")}</InfoLabel>
              <InfoValue>
                {challenge.visibility === "PRIVATE" ? t("challenges.private") : t("challenges.public")}
              </InfoValue>
            </InfoContent>
          </InfoItem>
        </InfoGrid>
      </InfoCard>

      {isParticipating && (
        <InfoCard>
          <CardTitle>
            <FiTrendingUp /> {t("challenges.yourProgress")}
          </CardTitle>
          <ProgressContainer>
            <ProgressLabel>
              <ProgressText>
                {percentage >= 100 ? t("challenges.completed") : t("challenges.inProgress")}
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
          <LeaderboardTitle>{t("challenges.leaderboard")}</LeaderboardTitle>
          <LeaderboardSubtitle>
            {leaderboard.length === 1
              ? t("challenges.participantsCount", { count: 1 })
              : t("challenges.participantsCountPlural", { count: leaderboard.length })}
          </LeaderboardSubtitle>
        </LeaderboardHeader>
        <LeaderboardList>
          {leaderboard.length === 0 ? (
            <EmptyLeaderboard>{t("challenges.empty.noParticipants")}</EmptyLeaderboard>
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
                    {isCurrentUser ? t("challenges.you") : entry.userName}
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
