import React, { useEffect, useState, useCallback } from "react";
import { FiPlus, FiSearch, FiActivity, FiMapPin, FiClock } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import ActivityService from "../api/ActivityService";
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
  color: #333;
`;

const SearchContainer = styled.div`
  position: relative;
  margin-bottom: 20px;
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  background-color: #f8f9fa;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
    background-color: white;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: #999;
  }
`;

const SearchIcon = styled.div`
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
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
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background-color: ${(props) => (props.active ? "#fc4c02" : "#f0f0f0")};
  color: ${(props) => (props.active ? "white" : "#666")};

  &:hover {
    background-color: ${(props) => (props.active ? "#e04402" : "#e0e0e0")};
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
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
`;

const StatIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background-color: ${(props) => props.color || "#fc4c02"}20;
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
  color: #333;
`;

const StatLabel = styled.div`
  font-size: 13px;
  color: #999;
  margin-top: 2px;
`;

const Activities = () => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState([]);
  const [filter, setFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
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
      const res = await ActivityService.getMyActivities(userId, 0, 50, searchQuery, activityType);
      setActivities(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error fetching activities:", error);
    } finally {
      setLoading(false);
    }
  }, [userId, searchQuery, filter]);

  const fetchStats = useCallback(async () => {
    try {
      const res = await ActivityService.getMyTotalStats(userId);
      setStats(res.data);
    } catch (error) {
      console.error("Error fetching stats:", error);
    }
  }, [userId]);

  useEffect(() => {
    fetchActivities();
  }, [fetchActivities]);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const handleLikeActivity = async (activityId) => {
    try {
      await ActivityService.likeActivity(activityId, userId);
      fetchActivities();
    } catch (error) {
      console.error("Error liking activity:", error);
    }
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const handleFilterChange = (newFilter) => {
    setFilter(newFilter);
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
        <PageTitle>My Activities</PageTitle>
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
          placeholder="Search activities..."
          value={searchQuery}
          onChange={handleSearchChange}
        />
      </SearchContainer>

      <FilterTabs>
        <FilterTab active={filter === "all"} onClick={() => handleFilterChange("all")}>
          All
        </FilterTab>
        <FilterTab active={filter === "RUN"} onClick={() => handleFilterChange("RUN")}>
          Run
        </FilterTab>
        <FilterTab active={filter === "RIDE"} onClick={() => handleFilterChange("RIDE")}>
          Ride
        </FilterTab>
        <FilterTab active={filter === "SWIM"} onClick={() => handleFilterChange("SWIM")}>
          Swim
        </FilterTab>
        <FilterTab active={filter === "HIKE"} onClick={() => handleFilterChange("HIKE")}>
          Hike
        </FilterTab>
        <FilterTab active={filter === "WALK"} onClick={() => handleFilterChange("WALK")}>
          Walk
        </FilterTab>
        <FilterTab active={filter === "WORKOUT"} onClick={() => handleFilterChange("WORKOUT")}>
          Workout
        </FilterTab>
      </FilterTabs>

      <StatsGrid>
        <StatCard>
          <StatIcon color="#fc4c02">
            <FiActivity size={24} />
          </StatIcon>
          <StatContent>
            <StatValue>{stats.totalActivities}</StatValue>
            <StatLabel>Activities</StatLabel>
          </StatContent>
        </StatCard>
        <StatCard>
          <StatIcon color="#2196f3">
            <FiMapPin size={24} />
          </StatIcon>
          <StatContent>
            <StatValue>{stats.totalDistanceKm?.toFixed(1) || 0}</StatValue>
            <StatLabel>Total km</StatLabel>
          </StatContent>
        </StatCard>
        <StatCard>
          <StatIcon color="#4caf50">
            <FiClock size={24} />
          </StatIcon>
          <StatContent>
            <StatValue>{formatDuration(stats.totalDurationSeconds || 0)}</StatValue>
            <StatLabel>Hours</StatLabel>
          </StatContent>
        </StatCard>
      </StatsGrid>

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
              : "No activities found. Start tracking your workouts!"}
          </EmptyText>
          {!searchQuery && (
            <AddButton to="/activities/new">
              <FiPlus size={18} />
              Add Activity
            </AddButton>
          )}
        </EmptyState>
      )}
    </Layout>
  );
};

export default Activities;
