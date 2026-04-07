import React, { useEffect, useState, useCallback, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  FiPlus,
  FiSearch,
  FiActivity,
  FiMapPin,
  FiClock,
  FiUsers,
  FiGlobe,
  FiUser,
} from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import ActivityService from "../api/ActivityService";
import UserService from "../api/UserService";
import ChallengeService from "../api/ChallengeService";
import { useAuth } from "../context/AuthContext";
import {
  ActivitiesList,
  EmptyState,
  EmptyIcon,
  EmptyText,
  AddButton,
} from "./DashboardStyles";
import { SkeletonActivityCard, SkeletonStatsCard } from "../components/Skeleton/Skeleton";
import { formatDuration } from "../utils/formatters";
import {
  PageHeader,
  PageTitle,
  SearchContainer,
  SearchInput,
  SearchIcon,
  MainTabs,
  MainTab,
  FilterTabs,
  FilterTab,
  ContentWrapper,
  FeedColumn,
  SidebarColumn,
  SidebarCard,
  SidebarTitle,
  AthletesList,
  AthleteItem,
  AthleteAvatar,
  AthleteInfo,
  AthleteName,
  AthleteStats,
  FollowButton,
  ChallengeCard,
  ChallengeHeader,
  ChallengeIcon,
  ChallengeTitle,
  ChallengeDescription,
  ProgressBox,
  ProgressHeader,
  ProgressLabel,
  ProgressValue,
  ProgressBar,
  ProgressFill,
  ParticipantsCount,
  StatsGrid,
  StatCard,
  StatIconWrapper,
  StatContent,
  StatValue,
  StatLabel,
} from "./ActivitiesStyles";

const Activities = () => {
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState([]);
  const [mainTab, setMainTab] = useState("all"); // all, following, my
  const [filter, setFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [suggestedAthletes, setSuggestedAthletes] = useState([]);
  const [activeChallenge, setActiveChallenge] = useState(null);
  const [stats, setStats] = useState({
    totalActivities: 0,
    totalDistanceKm: 0,
    totalDurationSeconds: 0,
  });
  const debounceTimerRef = useRef(null);

  // Debounce search input
  const handleSearchChange = useCallback((value) => {
    setSearchQuery(value);
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    debounceTimerRef.current = setTimeout(() => {
      setDebouncedSearch(value);
    }, 300);
  }, []);

  // Cleanup debounce timer on unmount
  useEffect(() => {
    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, []);

  const fetchActivities = useCallback(async () => {
    try {
      setLoading(true);
      const activityType = filter === "all" ? null : filter;

      let res;
      if (mainTab === "my") {
        res = await ActivityService.getMyActivities(
          0,
          50,
          debouncedSearch,
          activityType,
        );
      } else if (mainTab === "following") {
        res = await ActivityService.getFeedActivities(0, 50).catch(
          () => ({ data: [] }),
        );
      } else {
        res = await ActivityService.getPublicActivities(0, 50).catch(() => ({
          data: [],
        }));
      }

      let data = Array.isArray(res.data) ? res.data : [];

      // Apply client-side filtering for search and activity type on non-my tabs
      if (mainTab !== "my" && debouncedSearch) {
        const query = debouncedSearch.toLowerCase();
        data = data.filter(
          (a) =>
            a.title?.toLowerCase().includes(query) ||
            a.user?.firstName?.toLowerCase().includes(query) ||
            a.user?.lastName?.toLowerCase().includes(query),
        );
      }
      if (mainTab !== "my" && activityType) {
        data = data.filter((a) => a.activityType === activityType);
      }

      setActivities(data);
    } catch (error) {
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, filter, mainTab]);

  const fetchStats = useCallback(async () => {
    if (mainTab === "my") {
      try {
        const res = await ActivityService.getMyTotalStats();
        setStats(res.data);
      } catch (error) {
      }
    }
  }, [mainTab]);

  const fetchSuggestedAthletes = useCallback(async () => {
    try {
      const res = await UserService.getSuggestedUsers(5).catch(() => ({
        data: [],
      }));
      setSuggestedAthletes(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
    }
  }, []);

  const fetchActiveChallenge = useCallback(async () => {
    try {
      const res = await ChallengeService.getActiveChallenges(0, 10).catch(() => ({ data: [] }));
      const challenges = Array.isArray(res.data) ? res.data : [];
      if (challenges.length > 0) {
        setActiveChallenge(challenges[0]);
      }
    } catch (error) {
    }
  }, []);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    fetchSuggestedAthletes();
  }, [fetchSuggestedAthletes]);

  useEffect(() => {
    fetchActiveChallenge();
  }, [fetchActiveChallenge]);

  const handleLikeActivity = async (activityId, isLiked) => {
    try {
      if (isLiked) {
        await ActivityService.unlikeActivity(activityId);
      } else {
        await ActivityService.likeActivity(activityId);
      }
      setActivities(prev => prev.map(a =>
        a.id === activityId
          ? { ...a, likedByCurrentUser: !isLiked, likesCount: isLiked ? a.likesCount - 1 : a.likesCount + 1 }
          : a
      ));
    } catch (error) {
    }
  };

  const handleFollow = async (athleteId) => {
    try {
      await UserService.followUser(athleteId);
      fetchSuggestedAthletes();
    } catch (error) {
    }
  };

  const handleDeleteActivity = async (activity) => {
    if (!window.confirm(`Delete "${activity.title}"? This cannot be undone.`)) return;
    try {
      await ActivityService.deleteActivity(activity.id);
      setActivities((prev) => prev.filter((a) => a.id !== activity.id));
    } catch (error) {
    }
  };

  const handleEditActivity = (activity) => {
    navigate(`/activities/${activity.id}/edit`);
  };

  const isOwner = (activity) => currentUser && activity.user?.id === currentUser.id;

  if (loading && activities.length === 0) {
    return (
      <Layout>
        <PageHeader>
          <PageTitle>Activities</PageTitle>
          <AddButton to="/activities/new">
            <FiPlus size={18} />
            Add Activity
          </AddButton>
        </PageHeader>
        <StatsGrid>
          <SkeletonStatsCard />
          <SkeletonStatsCard />
          <SkeletonStatsCard />
        </StatsGrid>
        <ActivitiesList>
          <SkeletonActivityCard />
          <SkeletonActivityCard />
          <SkeletonActivityCard />
          <SkeletonActivityCard />
        </ActivitiesList>
      </Layout>
    );
  }

  return (
    <Layout>
      <PageHeader>
        <PageTitle>Activities</PageTitle>
        <AddButton to="/activities/new">
          <FiPlus size={18} />
          Add Activity
        </AddButton>
      </PageHeader>

      <SearchContainer>
        <SearchIcon>
          <FiSearch size={18} />
        </SearchIcon>
        <SearchInput
          type="text"
          placeholder="Search activities, athletes..."
          value={searchQuery}
          onChange={(e) => handleSearchChange(e.target.value)}
        />
      </SearchContainer>

      <MainTabs>
        <MainTab active={mainTab === "all"} onClick={() => setMainTab("all")}>
          <FiGlobe />
          All
        </MainTab>
        <MainTab
          active={mainTab === "following"}
          onClick={() => setMainTab("following")}
        >
          <FiUsers />
          Following
        </MainTab>
        <MainTab active={mainTab === "my"} onClick={() => setMainTab("my")}>
          <FiUser />
          My Activities
        </MainTab>
      </MainTabs>

      <FilterTabs>
        <FilterTab active={filter === "all"} onClick={() => setFilter("all")}>
          All Types
        </FilterTab>
        <FilterTab active={filter === "RUN"} onClick={() => setFilter("RUN")}>
          Run
        </FilterTab>
        <FilterTab active={filter === "RIDE"} onClick={() => setFilter("RIDE")}>
          Ride
        </FilterTab>
        <FilterTab active={filter === "SWIM"} onClick={() => setFilter("SWIM")}>
          Swim
        </FilterTab>
        <FilterTab active={filter === "HIKE"} onClick={() => setFilter("HIKE")}>
          Hike
        </FilterTab>
        <FilterTab
          active={filter === "WORKOUT"}
          onClick={() => setFilter("WORKOUT")}
        >
          Workout
        </FilterTab>
      </FilterTabs>

      {mainTab === "my" && (
        <StatsGrid>
          <StatCard>
            <StatIconWrapper color="#fc4c02">
              <FiActivity size={24} />
            </StatIconWrapper>
            <StatContent>
              <StatValue>{stats.totalActivities || 0}</StatValue>
              <StatLabel>Activities</StatLabel>
            </StatContent>
          </StatCard>
          <StatCard>
            <StatIconWrapper color="#3b82f6">
              <FiMapPin size={24} />
            </StatIconWrapper>
            <StatContent>
              <StatValue>{stats.totalDistanceKm?.toFixed(1) || 0}</StatValue>
              <StatLabel>Total km</StatLabel>
            </StatContent>
          </StatCard>
          <StatCard>
            <StatIconWrapper color="#22c55e">
              <FiClock size={24} />
            </StatIconWrapper>
            <StatContent>
              <StatValue>
                {formatDuration(stats.totalDurationSeconds || 0)}
              </StatValue>
              <StatLabel>Total Time</StatLabel>
            </StatContent>
          </StatCard>
        </StatsGrid>
      )}

      <ContentWrapper>
        <FeedColumn>
          {activities.length > 0 ? (
            <ActivitiesList>
              {activities.map((activity) => (
                <ActivityCard
                  key={activity.id}
                  activity={activity}
                  onLike={handleLikeActivity}
                  onEdit={isOwner(activity) ? handleEditActivity : undefined}
                  onDelete={isOwner(activity) ? handleDeleteActivity : undefined}
                />
              ))}
            </ActivitiesList>
          ) : (
            <EmptyState>
              <EmptyIcon>🏃</EmptyIcon>
              <EmptyText>
                {searchQuery
                  ? "No activities found matching your search."
                  : mainTab === "following"
                    ? "No activities from people you follow yet."
                    : mainTab === "my"
                      ? "No activities yet. Start tracking your workouts!"
                      : "No activities found. Check back later!"}
              </EmptyText>
              {mainTab === "my" && !searchQuery && (
                <AddButton to="/activities/new">
                  <FiPlus size={18} />
                  Add Activity
                </AddButton>
              )}
            </EmptyState>
          )}
        </FeedColumn>

        <SidebarColumn>
          {suggestedAthletes.length > 0 && (
            <SidebarCard>
              <SidebarTitle>Athletes to Follow</SidebarTitle>
              <AthletesList>
                {suggestedAthletes.map((athlete) => (
                  <AthleteItem key={athlete.id}>
                    <AthleteAvatar to={`/profile/${athlete.id}`}>
                      {athlete.avatar ? (
                        <img src={athlete.avatar} alt={athlete.firstName} />
                      ) : (
                        athlete.firstName?.charAt(0)
                      )}
                    </AthleteAvatar>
                    <AthleteInfo>
                      <AthleteName to={`/profile/${athlete.id}`}>
                        {athlete.firstName} {athlete.lastName}
                      </AthleteName>
                      <AthleteStats>Athlete</AthleteStats>
                    </AthleteInfo>
                    <FollowButton onClick={() => handleFollow(athlete.id)}>
                      Follow
                    </FollowButton>
                  </AthleteItem>
                ))}
              </AthletesList>
            </SidebarCard>
          )}
          {activeChallenge && (
            <ChallengeCard to={`/challenges`}>
              <ChallengeHeader>
                <ChallengeIcon>🔥</ChallengeIcon>
                <ChallengeTitle>{activeChallenge.name}</ChallengeTitle>
              </ChallengeHeader>
              <ChallengeDescription>
                {activeChallenge.description}
              </ChallengeDescription>
              <ProgressBox>
                <ProgressHeader>
                  <ProgressLabel>Your progress</ProgressLabel>
                  <ProgressValue>
                    {activeChallenge.userProgress?.toFixed(1) || 0} /{" "}
                    {activeChallenge.targetValue}{" "}
                    {activeChallenge.challengeType === "ELEVATION" ? "m" : "km"}
                  </ProgressValue>
                </ProgressHeader>
                <ProgressBar>
                  <ProgressFill
                    progress={
                      ((activeChallenge.userProgress || 0) /
                        activeChallenge.targetValue) *
                      100
                    }
                  />
                </ProgressBar>
              </ProgressBox>
              <ParticipantsCount>
                {activeChallenge.participantsCount?.toLocaleString() || 0}{" "}
                participants
              </ParticipantsCount>
            </ChallengeCard>
          )}
        </SidebarColumn>
      </ContentWrapper>
    </Layout>
  );
};

export default Activities;
