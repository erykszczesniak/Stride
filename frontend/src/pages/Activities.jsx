import React, { useEffect, useState } from "react";
import { FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import ActivityService from "../api/ActivityService";
import {
  Section,
  SectionHeader,
  SectionTitle,
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

const FilterTabs = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
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

const Activities = () => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState([]);
  const [filter, setFilter] = useState("all");
  const userId = 1;

  useEffect(() => {
    const fetchActivities = async () => {
      try {
        setLoading(true);
        const res = await ActivityService.getMyActivities(userId, 0, 20);
        setActivities(Array.isArray(res.data) ? res.data : []);
      } catch (error) {
        console.error("Error fetching activities:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchActivities();
  }, [userId]);

  const handleLikeActivity = async (activityId) => {
    try {
      await ActivityService.likeActivity(activityId, userId);
      const res = await ActivityService.getMyActivities(userId, 0, 20);
      setActivities(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error liking activity:", error);
    }
  };

  const filteredActivities = filter === "all"
    ? activities
    : activities.filter(a => a.activityType === filter);

  if (loading) {
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

      <FilterTabs>
        <FilterTab active={filter === "all"} onClick={() => setFilter("all")}>
          All
        </FilterTab>
        <FilterTab active={filter === "RUN"} onClick={() => setFilter("RUN")}>
          Running
        </FilterTab>
        <FilterTab active={filter === "RIDE"} onClick={() => setFilter("RIDE")}>
          Cycling
        </FilterTab>
        <FilterTab active={filter === "SWIM"} onClick={() => setFilter("SWIM")}>
          Swimming
        </FilterTab>
      </FilterTabs>

      {filteredActivities.length > 0 ? (
        <ActivitiesList>
          {filteredActivities.map((activity) => (
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
          <EmptyText>No activities found. Start tracking your workouts!</EmptyText>
          <AddButton to="/activities/new">
            <FiPlus size={18} />
            Add Activity
          </AddButton>
        </EmptyState>
      )}
    </Layout>
  );
};

export default Activities;
