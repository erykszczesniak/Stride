import React, { useEffect, useState, useCallback } from "react";
import { FiPlus, FiSearch, FiActivity, FiMapPin, FiClock, FiUsers, FiGlobe, FiUser } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import ActivityService from "../api/ActivityService";
import UserService from "../api/UserService";
import { Link } from "react-router-dom";
import {
  ActivitiesList,
  EmptyState,
  EmptyIcon,
  EmptyText,
  AddButton,
  LoadingSpinner,
} from "./DashboardStyles";
import styled from "styled-components";

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
`;

const SearchContainer = styled.div`
  position: relative;
  margin-bottom: 20px;
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  font-size: 15px;
  background-color: #f8fafc;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
    background-color: white;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: #94a3b8;
  }
`;

const SearchIcon = styled.div`
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
`;

const MainTabs = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 12px;
`;

const MainTab = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? "#fc4c02" : "transparent")};
  color: ${(props) => (props.active ? "white" : "#64748b")};

  &:hover {
    background-color: ${(props) => (props.active ? "#e04402" : "#f1f5f9")};
  }

  svg {
    width: 18px;
    height: 18px;
  }
`;

const FilterTabs = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
`;

const FilterTab = styled.button`
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? "#fff7ed" : "#f1f5f9")};
  color: ${(props) => (props.active ? "#fc4c02" : "#64748b")};
  border: 1px solid ${(props) => (props.active ? "#fc4c02" : "transparent")};

  &:hover {
    background-color: ${(props) => (props.active ? "#fff7ed" : "#e2e8f0")};
  }
`;

const ContentWrapper = styled.div`
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: 1fr;
  }
`;

const FeedColumn = styled.div``;

const SidebarColumn = styled.div`
  @media (max-width: 1024px) {
    display: none;
  }
`;

const SidebarCard = styled.div`
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
`;

const SidebarTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
`;

const AthletesList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const AthleteItem = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const AthleteAvatar = styled(Link)`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  text-decoration: none;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

const AthleteInfo = styled.div`
  flex: 1;
  min-width: 0;
`;

const AthleteName = styled(Link)`
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  text-decoration: none;
  display: block;

  &:hover {
    color: #fc4c02;
  }
`;

const AthleteStats = styled.span`
  font-size: 12px;
  color: #94a3b8;
`;

const FollowButton = styled.button`
  padding: 6px 14px;
  background: #fc4c02;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #e04402;
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const StatCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
`;

const StatIconWrapper = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background-color: ${(props) => props.color || "#fc4c02"}15;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${(props) => props.color || "#fc4c02"};
`;

const StatContent = styled.div`
  flex: 1;
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
`;

const StatLabel = styled.div`
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
`;

const Activities = () => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState([]);
  const [mainTab, setMainTab] = useState("all"); // all, following, my
  const [filter, setFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [suggestedAthletes, setSuggestedAthletes] = useState([]);
  const [stats, setStats] = useState({
    totalActivities: 0,
    totalDistanceKm: 0,
    totalDurationSeconds: 0,
  });
  const userId = 1;

  const fetchActivities = useCallback(async () => {
    try {
      setLoading(true);
      const activityType = filter === "all" ? null : filter;

      let res;
      if (mainTab === "my") {
        res = await ActivityService.getMyActivities(userId, 0, 50, searchQuery, activityType);
      } else if (mainTab === "following") {
        res = await ActivityService.getFeedActivities(userId, 0, 50).catch(() => ({ data: [] }));
      } else {
        res = await ActivityService.getPublicActivities(0, 50).catch(() => ({ data: [] }));
      }

      let data = Array.isArray(res.data) ? res.data : [];

      // Apply client-side filtering for search and activity type on non-my tabs
      if (mainTab !== "my" && searchQuery) {
        data = data.filter(a =>
          a.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          a.user?.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          a.user?.lastName?.toLowerCase().includes(searchQuery.toLowerCase())
        );
      }
      if (mainTab !== "my" && activityType) {
        data = data.filter(a => a.activityType === activityType);
      }

      setActivities(data);
    } catch (error) {
      console.error("Error fetching activities:", error);
    } finally {
      setLoading(false);
    }
  }, [userId, searchQuery, filter, mainTab]);

  const fetchStats = useCallback(async () => {
    if (mainTab === "my") {
      try {
        const res = await ActivityService.getMyTotalStats(userId);
        setStats(res.data);
      } catch (error) {
        console.error("Error fetching stats:", error);
      }
    }
  }, [userId, mainTab]);

  const fetchSuggestedAthletes = useCallback(async () => {
    try {
      const res = await UserService.getSuggestedUsers(userId, 5).catch(() => ({ data: [] }));
      setSuggestedAthletes(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error fetching suggested athletes:", error);
    }
  }, [userId]);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    fetchSuggestedAthletes();
  }, [fetchSuggestedAthletes]);

  const handleLikeActivity = async (activityId) => {
    try {
      await ActivityService.likeActivity(activityId, userId);
      fetchActivities();
    } catch (error) {
      console.error("Error liking activity:", error);
    }
  };

  const handleFollow = async (athleteId) => {
    try {
      await UserService.followUser(userId, athleteId);
      fetchSuggestedAthletes();
    } catch (error) {
      console.error("Error following user:", error);
    }
  };

  const formatDuration = (seconds) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  if (loading && activities.length === 0) {
    return (
      <Layout>
        <LoadingSpinner />
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
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </SearchContainer>

      <MainTabs>
        <MainTab active={mainTab === "all"} onClick={() => setMainTab("all")}>
          <FiGlobe />
          All
        </MainTab>
        <MainTab active={mainTab === "following"} onClick={() => setMainTab("following")}>
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
        <FilterTab active={filter === "WORKOUT"} onClick={() => setFilter("WORKOUT")}>
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
              <StatValue>{formatDuration(stats.totalDurationSeconds || 0)}</StatValue>
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
        </SidebarColumn>
      </ContentWrapper>
    </Layout>
  );
};

export default Activities;
