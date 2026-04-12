import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiUsers, FiCalendar, FiAward, FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ChallengeService from "../api/ChallengeService";
import { useAuth } from "../context/AuthContext";
import { getIconForType, formatUnit, getDaysLeft, getAvatarColor } from "../utils/challengeHelpers";
import {
  PageHeader,
  HeaderLeft,
  PageTitle,
  PageSubtitle,
  HeaderRight,
  CreateButton,
  ActiveBadge,
  TabsContainer,
  Tab,
  TabBadge,
  ChallengeCard,
  ChallengeIcon,
  ChallengeContent,
  ChallengeTitle,
  ChallengeDescription,
  ChallengeMeta,
  MetaItem,
  Badge,
  ProgressContainer,
  ProgressLabel,
  ProgressText,
  ProgressValue,
  ProgressBar,
  ProgressFill,
  JoinButton,
  LeaveButton,
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
  EmptyState,
  EmptyIcon,
  EmptyText,
  LoadingSpinner,
  ChallengeSelect,
} from "./ChallengesStyles";

const Challenges = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState("my");
  const [myChallenges, setMyChallenges] = useState([]);
  const [exploreChallenges, setExploreChallenges] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [selectedChallenge, setSelectedChallenge] = useState(null);
  const [loading, setLoading] = useState(true);
  const [joiningId, setJoiningId] = useState(null);
  const [leavingId, setLeavingId] = useState(null);

  const fetchMyChallenges = useCallback(async () => {
    try {
      const res = await ChallengeService.getMyChallenges();
      setMyChallenges(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
    }
  }, []);

  const fetchExploreChallenges = useCallback(async () => {
    try {
      const res = await ChallengeService.getActiveChallenges();
      const challenges = Array.isArray(res.data) ? res.data : [];
      // Filter out challenges user is already participating in
      setExploreChallenges(challenges.filter(c => !c.participating));
    } catch (error) {
    }
  }, []);

  const fetchLeaderboard = useCallback(async (challengeId) => {
    try {
      const res = await ChallengeService.getLeaderboard(challengeId);
      setLeaderboard(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
    }
  }, []);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      await Promise.all([fetchMyChallenges(), fetchExploreChallenges()]);
      setLoading(false);
    };
    fetchData();
  }, [fetchMyChallenges, fetchExploreChallenges]);

  useEffect(() => {
    if (activeTab === "leaderboard" && myChallenges.length > 0) {
      if (!selectedChallenge) {
        setSelectedChallenge(myChallenges[0]);
      }
    }
  }, [activeTab, myChallenges, selectedChallenge]);

  useEffect(() => {
    if (activeTab === "leaderboard" && selectedChallenge) {
      fetchLeaderboard(selectedChallenge.id);
    }
  }, [activeTab, selectedChallenge, fetchLeaderboard]);

  const handleJoinChallenge = async (challengeId) => {
    setJoiningId(challengeId);
    try {
      await ChallengeService.joinChallenge(challengeId);
      await Promise.all([fetchMyChallenges(), fetchExploreChallenges()]);
    } catch (error) {
    } finally {
      setJoiningId(null);
    }
  };

  const handleLeaveChallenge = async (challengeId) => {
    setLeavingId(challengeId);
    try {
      await ChallengeService.leaveChallenge(challengeId);
      await Promise.all([fetchMyChallenges(), fetchExploreChallenges()]);
    } catch (error) {
    } finally {
      setLeavingId(null);
    }
  };

  const handleChallengeSelect = (e) => {
    const challenge = myChallenges.find(c => c.id === parseInt(e.target.value));
    if (challenge) {
      setSelectedChallenge(challenge);
      fetchLeaderboard(challenge.id);
    }
  };

  const getIconColors = (index) => {
    const colors = [
      { bg: "#fff3ed", icon: "#fc4c02" },
      { bg: "#e8f5e9", icon: "#4caf50" },
      { bg: "#e3f2fd", icon: "#2196f3" },
      { bg: "#fff8e1", icon: "#ff9800" },
      { bg: "#f3e5f5", icon: "#9c27b0" },
    ];
    return colors[index % colors.length];
  };

  const renderMyChallenges = () => {
    if (myChallenges.length === 0) {
      return (
        <EmptyState>
          <EmptyIcon>🏆</EmptyIcon>
          <EmptyText>{t("challenges.empty.my")}</EmptyText>
        </EmptyState>
      );
    }

    return myChallenges.map((challenge, index) => {
      const colors = getIconColors(index);
      const progress = challenge.userProgress || 0;
      const percentage = (progress / challenge.targetValue) * 100;
      const unit = formatUnit(challenge.challengeType);

      return (
        <ChallengeCard key={challenge.id} onClick={() => navigate(`/challenges/${challenge.id}`)}>
          <ChallengeIcon bgColor={colors.bg} iconColor={colors.icon}>
            {getIconForType(challenge.challengeType, challenge.activityType)}
          </ChallengeIcon>
          <ChallengeContent>
            <ChallengeTitle>{challenge.name}</ChallengeTitle>
            <ChallengeDescription>{challenge.description}</ChallengeDescription>
            <ChallengeMeta>
              <MetaItem>
                <FiUsers size={14} />
                {challenge.participantsCount}
              </MetaItem>
              <MetaItem>
                <FiCalendar size={14} />
                {t("challenges.daysLeft", { count: getDaysLeft(challenge.endDate) })}
              </MetaItem>
              <Badge>
                <FiAward size={12} />
                {t("challenges.challengeBadge")}
              </Badge>
            </ChallengeMeta>
            <ProgressContainer>
              <ProgressLabel>
                <ProgressText>{t("challenges.progress")}</ProgressText>
                <ProgressValue>{progress.toFixed(1)} / {challenge.targetValue} {unit}</ProgressValue>
              </ProgressLabel>
              <ProgressBar>
                <ProgressFill percentage={percentage} />
              </ProgressBar>
            </ProgressContainer>
          </ChallengeContent>
          {challenge.creatorId !== user?.id && (
            <LeaveButton
              onClick={(e) => { e.stopPropagation(); handleLeaveChallenge(challenge.id); }}
              disabled={leavingId === challenge.id}
            >
              {leavingId === challenge.id ? t("challenges.leaving") : t("challenges.leave")}
            </LeaveButton>
          )}
        </ChallengeCard>
      );
    });
  };

  const renderExploreChallenges = () => {
    if (exploreChallenges.length === 0) {
      return (
        <EmptyState>
          <EmptyIcon>🔍</EmptyIcon>
          <EmptyText>{t("challenges.empty.explore")}</EmptyText>
        </EmptyState>
      );
    }

    return exploreChallenges.map((challenge, index) => {
      const colors = getIconColors(index);

      return (
        <ChallengeCard key={challenge.id} onClick={() => navigate(`/challenges/${challenge.id}`)}>
          <ChallengeIcon bgColor={colors.bg} iconColor={colors.icon}>
            {getIconForType(challenge.challengeType, challenge.activityType)}
          </ChallengeIcon>
          <ChallengeContent>
            <ChallengeTitle>{challenge.name}</ChallengeTitle>
            <ChallengeDescription>{challenge.description}</ChallengeDescription>
            <ChallengeMeta>
              <MetaItem>
                <FiUsers size={14} />
                {challenge.participantsCount}
              </MetaItem>
              <MetaItem>
                <FiCalendar size={14} />
                {t("challenges.daysLeft", { count: getDaysLeft(challenge.endDate) })}
              </MetaItem>
              <Badge>
                <FiAward size={12} />
                {t("challenges.challengeBadge")}
              </Badge>
            </ChallengeMeta>
          </ChallengeContent>
          <JoinButton
            onClick={(e) => { e.stopPropagation(); handleJoinChallenge(challenge.id); }}
            disabled={joiningId === challenge.id}
          >
            {joiningId === challenge.id ? t("challenges.joining") : t("challenges.join")}
          </JoinButton>
        </ChallengeCard>
      );
    });
  };

  const renderLeaderboard = () => {
    if (myChallenges.length === 0) {
      return (
        <EmptyState>
          <EmptyIcon>📊</EmptyIcon>
          <EmptyText>{t("challenges.empty.leaderboard")}</EmptyText>
        </EmptyState>
      );
    }

    const unit = selectedChallenge ? formatUnit(selectedChallenge.challengeType) : "";

    return (
      <>
        <ChallengeSelect value={selectedChallenge?.id || ""} onChange={handleChallengeSelect}>
          {myChallenges.map(c => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </ChallengeSelect>

        <LeaderboardContainer>
          <LeaderboardHeader>
            <LeaderboardTitle>{selectedChallenge?.name}</LeaderboardTitle>
            <LeaderboardSubtitle>{t("challenges.topPerformers")}</LeaderboardSubtitle>
          </LeaderboardHeader>
          <LeaderboardList>
            {leaderboard.length === 0 ? (
              <LeaderboardItem>
                <UserName>{t("challenges.empty.noParticipants")}</UserName>
              </LeaderboardItem>
            ) : (
              leaderboard.map((entry, index) => {
                const isCurrentUser = entry.isCurrentUser;
                const isTop3 = index < 3;
                return (
                  <LeaderboardItem key={entry.userId} isCurrentUser={isCurrentUser}>
                    <Rank isTop3={isTop3}>
                      {isTop3 ? "🏆" : index + 1}
                    </Rank>
                    <Avatar bgColor={getAvatarColor(index)}>
                      {entry.userName?.charAt(0) || "?"}
                    </Avatar>
                    <UserName isCurrentUser={isCurrentUser}>
                      {isCurrentUser ? t("challenges.you") : entry.userName}
                    </UserName>
                    <UserScore>{entry.progress?.toFixed(1)} {unit}</UserScore>
                  </LeaderboardItem>
                );
              })
            )}
          </LeaderboardList>
        </LeaderboardContainer>
      </>
    );
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner>{t("challenges.loading")}</LoadingSpinner>
      </Layout>
    );
  }

  return (
    <Layout>
      <PageHeader>
        <HeaderLeft>
          <PageTitle>{t("challenges.title")}</PageTitle>
          <PageSubtitle>{t("challenges.subtitle")}</PageSubtitle>
        </HeaderLeft>
        <HeaderRight>
          <ActiveBadge>
            <FiAward size={18} />
            {myChallenges.length} {t("challenges.active")}
          </ActiveBadge>
          <CreateButton onClick={() => navigate("/challenges/new")}>
            <FiPlus size={16} />
            {t("challenges.create.createButton")}
          </CreateButton>
        </HeaderRight>
      </PageHeader>

      <TabsContainer>
        <Tab active={activeTab === "my"} onClick={() => setActiveTab("my")}>
          {t("challenges.tabs.my")}
        </Tab>
        <Tab active={activeTab === "explore"} onClick={() => setActiveTab("explore")}>
          {t("challenges.tabs.explore")}
          {exploreChallenges.length > 0 && <TabBadge />}
        </Tab>
        <Tab active={activeTab === "leaderboard"} onClick={() => setActiveTab("leaderboard")}>
          {t("challenges.tabs.leaderboard")}
        </Tab>
      </TabsContainer>

      {activeTab === "my" && renderMyChallenges()}
      {activeTab === "explore" && renderExploreChallenges()}
      {activeTab === "leaderboard" && renderLeaderboard()}
    </Layout>
  );
};

export default Challenges;
