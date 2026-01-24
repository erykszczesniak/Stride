import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { FiArrowLeft, FiHeart, FiMessageCircle, FiShare2, FiMapPin, FiClock } from "react-icons/fi";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import Layout from "../components/Layout/Layout";
import ActivityService from "../api/ActivityService";
import { LoadingSpinner } from "./DashboardStyles";
import styled from "styled-components";

const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;

  &:hover {
    color: #fc4c02;
  }
`;

const ActivityHeader = styled.div`
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const UserSection = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

const Avatar = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #fc4c02;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
`;

const UserInfo = styled.div``;

const UserName = styled.p`
  font-weight: 600;
  color: #333;
`;

const ActivityTime = styled.p`
  font-size: 13px;
  color: #666;
`;

const Title = styled.h1`
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
`;

const Description = styled.p`
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
`;

const StatItem = styled.div`
  text-align: center;
`;

const StatValue = styled.p`
  font-size: 24px;
  font-weight: 700;
  color: #333;
`;

const StatLabel = styled.p`
  font-size: 12px;
  color: #666;
  text-transform: uppercase;
`;

const ActionsBar = styled.div`
  display: flex;
  gap: 16px;
`;

const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background-color: #f5f5f5;
  }
`;

const activityIcons = {
  RUN: <FaRunning size={20} />,
  RIDE: <FaBicycle size={20} />,
  SWIM: <FaSwimmer size={20} />,
  HIKE: <FaHiking size={20} />,
};

const formatDuration = (seconds) => {
  if (!seconds) return "0m";
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
};

const ActivityDetails = () => {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [activity, setActivity] = useState(null);

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        setLoading(true);
        const res = await ActivityService.getActivityById(id, 1);
        setActivity(res.data);
      } catch (error) {
        console.error("Error fetching activity:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchActivity();
  }, [id]);

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  if (!activity) {
    return (
      <Layout>
        <p>Activity not found</p>
      </Layout>
    );
  }

  return (
    <Layout>
      <BackLink to="/activities">
        <FiArrowLeft size={18} />
        Back to Activities
      </BackLink>

      <ActivityHeader>
        <UserSection>
          <Avatar>
            {activity.user?.firstName?.charAt(0) || "U"}
          </Avatar>
          <UserInfo>
            <UserName>
              {activity.user?.firstName} {activity.user?.lastName}
            </UserName>
            <ActivityTime>
              {new Date(activity.createdAt).toLocaleDateString()}
            </ActivityTime>
          </UserInfo>
        </UserSection>

        <Title>{activity.title}</Title>
        {activity.description && <Description>{activity.description}</Description>}

        <StatsGrid>
          <StatItem>
            <StatValue>{activity.distance?.toFixed(2) || 0}</StatValue>
            <StatLabel>Distance (km)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{formatDuration(activity.duration)}</StatValue>
            <StatLabel>Duration</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.elevationGain || 0}</StatValue>
            <StatLabel>Elevation (m)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.calories || 0}</StatValue>
            <StatLabel>Calories</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.averageSpeed?.toFixed(1) || 0}</StatValue>
            <StatLabel>Avg Speed (km/h)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.averageHeartRate || "-"}</StatValue>
            <StatLabel>Avg HR (bpm)</StatLabel>
          </StatItem>
        </StatsGrid>

        <ActionsBar>
          <ActionButton>
            <FiHeart size={18} />
            {activity.likesCount || 0} Likes
          </ActionButton>
          <ActionButton>
            <FiMessageCircle size={18} />
            {activity.commentsCount || 0} Comments
          </ActionButton>
          <ActionButton>
            <FiShare2 size={18} />
            Share
          </ActionButton>
        </ActionsBar>
      </ActivityHeader>
    </Layout>
  );
};

export default ActivityDetails;
