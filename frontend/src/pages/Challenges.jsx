import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
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
          <EmptyText>You haven't joined any challenges yet. Explore and join one!</EmptyText>
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
                {getDaysLeft(challenge.endDate)} days left
              </MetaItem>
              <Badge>
                <FiAward size={12} />
                Challenge Badge
              </Badge>
            </ChallengeMeta>
            <ProgressContainer>
              <ProgressLabel>
                <ProgressText>Progress</ProgressText>
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
              {leavingId === challenge.id ? "Leaving..." : "Leave"}
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
          <EmptyText>No new challenges available at the moment.</EmptyText>
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
                {getDaysLeft(challenge.endDate)} days left
              </MetaItem>
              <Badge>
                <FiAward size={12} />
                Challenge Badge
              </Badge>
            </ChallengeMeta>
          </ChallengeContent>
          <JoinButton
            onClick={(e) => { e.stopPropagation(); handleJoinChallenge(challenge.id); }}
            disabled={joiningId === challenge.id}
          >
            {joiningId === challenge.id ? "Joining..." : "Join"}
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
          <EmptyText>Join a challenge to see the leaderboard.</EmptyText>
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
            <LeaderboardSubtitle>Top performers this month</LeaderboardSubtitle>
          </LeaderboardHeader>
          <LeaderboardList>
            {leaderboard.length === 0 ? (
              <LeaderboardItem>
                <UserName>No participants yet</UserName>
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
                      {isCurrentUser ? "You" : entry.userName}
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
        <LoadingSpinner>Loading challenges...</LoadingSpinner>
      </Layout>
    );
  }

  return (
    <Layout>
      <PageHeader>
        <HeaderLeft>
          <PageTitle>Challenges</PageTitle>
          <PageSubtitle>Compete and earn rewards</PageSubtitle>
        </HeaderLeft>
        <HeaderRight>
          <ActiveBadge>
            <FiAward size={18} />
            {myChallenges.length} Active
          </ActiveBadge>
          <CreateButton onClick={() => navigate("/challenges/new")}>
            <FiPlus size={16} />
            Create
          </CreateButton>
        </HeaderRight>
      </PageHeader>

      <TabsContainer>
        <Tab active={activeTab === "my"} onClick={() => setActiveTab("my")}>
          My Challenges
        </Tab>
        <Tab active={activeTab === "explore"} onClick={() => setActiveTab("explore")}>
          Explore
          {exploreChallenges.length > 0 && <TabBadge />}
        </Tab>
        <Tab active={activeTab === "leaderboard"} onClick={() => setActiveTab("leaderboard")}>
          Leaderboard
        </Tab>
      </TabsContainer>

      {activeTab === "my" && renderMyChallenges()}
      {activeTab === "explore" && renderExploreChallenges()}
      {activeTab === "leaderboard" && renderLeaderboard()}
    </Layout>
  );
};

export default Challenges;
