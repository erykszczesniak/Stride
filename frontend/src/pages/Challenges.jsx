import React, { useEffect, useState, useCallback } from "react";
import { FiUsers, FiCalendar, FiAward } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ChallengeService from "../api/ChallengeService";
import styled from "styled-components";

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
`;

const HeaderLeft = styled.div``;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0;
`;

const PageSubtitle = styled.p`
  font-size: 14px;
  color: #666;
  margin: 4px 0 0 0;
`;

const ActiveBadge = styled.div`
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

const TabsContainer = styled.div`
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background-color: #f5f5f5;
  padding: 4px;
  border-radius: 10px;
  width: fit-content;
`;

const Tab = styled.button`
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? "white" : "transparent")};
  color: ${(props) => (props.active ? "#333" : "#666")};
  box-shadow: ${(props) => (props.active ? "0 1px 3px rgba(0,0,0,0.1)" : "none")};
  position: relative;

  &:hover {
    color: #333;
  }
`;

const TabBadge = styled.span`
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  background-color: #fc4c02;
  border-radius: 50%;
`;

const ChallengeCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: flex-start;
  gap: 16px;
`;

const ChallengeIcon = styled.div`
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

const ChallengeContent = styled.div`
  flex: 1;
  min-width: 0;
`;

const ChallengeTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
`;

const ChallengeDescription = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
`;

const ChallengeMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
`;

const MetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
`;

const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background-color: #f5f5f5;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  color: #333;
`;

const ProgressContainer = styled.div`
  margin-top: 16px;
`;

const ProgressLabel = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

const ProgressText = styled.span`
  font-size: 13px;
  color: #666;
`;

const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: #333;
`;

const ProgressBar = styled.div`
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
`;

const ProgressFill = styled.div`
  height: 100%;
  background-color: #fc4c02;
  border-radius: 4px;
  width: ${(props) => Math.min(props.percentage, 100)}%;
  transition: width 0.3s ease;
`;

const JoinButton = styled.button`
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

const LeaderboardContainer = styled.div`
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
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
  border-bottom: 1px solid #f0f0f0;
  background-color: ${(props) => (props.isCurrentUser ? "#fff8f5" : "white")};

  &:last-child {
    border-bottom: none;
  }
`;

const Rank = styled.div`
  width: 32px;
  font-size: 14px;
  font-weight: 500;
  color: ${(props) => (props.isTop3 ? "#fc4c02" : "#666")};
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
  color: ${(props) => (props.isCurrentUser ? "#fc4c02" : "#333")};
`;

const UserScore = styled.span`
  font-size: 15px;
  font-weight: 600;
  color: #333;
`;

const EmptyState = styled.div`
  background: white;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

const EmptyText = styled.p`
  font-size: 16px;
  color: #666;
  margin: 0;
`;

const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  padding: 60px;
  color: #666;
`;

const ChallengeSelect = styled.select`
  padding: 10px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
  background: white;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

const Challenges = () => {
  const [activeTab, setActiveTab] = useState("my");
  const [myChallenges, setMyChallenges] = useState([]);
  const [exploreChallenges, setExploreChallenges] = useState([]);
  const [leaderboard, setLeaderboard] = useState([]);
  const [selectedChallenge, setSelectedChallenge] = useState(null);
  const [loading, setLoading] = useState(true);
  const [joiningId, setJoiningId] = useState(null);

  const fetchMyChallenges = useCallback(async () => {
    try {
      const res = await ChallengeService.getMyChallenges();
      setMyChallenges(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error fetching my challenges:", error);
    }
  }, []);

  const fetchExploreChallenges = useCallback(async () => {
    try {
      const res = await ChallengeService.getActiveChallenges();
      const challenges = Array.isArray(res.data) ? res.data : [];
      // Filter out challenges user is already participating in
      setExploreChallenges(challenges.filter(c => !c.participating));
    } catch (error) {
      console.error("Error fetching explore challenges:", error);
    }
  }, []);

  const fetchLeaderboard = useCallback(async (challengeId) => {
    try {
      const res = await ChallengeService.getLeaderboard(challengeId);
      setLeaderboard(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error fetching leaderboard:", error);
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
      const challenge = selectedChallenge || myChallenges[0];
      setSelectedChallenge(challenge);
      fetchLeaderboard(challenge.id);
    }
  }, [activeTab, myChallenges, selectedChallenge, fetchLeaderboard]);

  const handleJoinChallenge = async (challengeId) => {
    setJoiningId(challengeId);
    try {
      await ChallengeService.joinChallenge(challengeId);
      await Promise.all([fetchMyChallenges(), fetchExploreChallenges()]);
    } catch (error) {
      console.error("Error joining challenge:", error);
    } finally {
      setJoiningId(null);
    }
  };

  const handleChallengeSelect = (e) => {
    const challenge = myChallenges.find(c => c.id === parseInt(e.target.value));
    if (challenge) {
      setSelectedChallenge(challenge);
      fetchLeaderboard(challenge.id);
    }
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

  const getAvatarColor = (index) => {
    const colors = ["#5c6bc0", "#26a69a", "#ef5350", "#7e57c2", "#42a5f5"];
    return colors[index % colors.length];
  };

  const formatUnit = (challengeType) => {
    switch (challengeType) {
      case "DISTANCE": return "km";
      case "TIME": return "hours";
      case "ELEVATION": return "m";
      case "ACTIVITIES_COUNT": return "days";
      default: return "";
    }
  };

  const getDaysLeft = (endDate) => {
    const end = new Date(endDate);
    const now = new Date();
    const diff = Math.ceil((end - now) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 0;
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
        <ChallengeCard key={challenge.id}>
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
        <ChallengeCard key={challenge.id}>
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
            onClick={() => handleJoinChallenge(challenge.id)}
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
        <ActiveBadge>
          <FiAward size={18} />
          {myChallenges.length} Active
        </ActiveBadge>
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
