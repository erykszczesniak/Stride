import React, { useState } from "react";
import { FiHeart, FiMessageCircle, FiShare2, FiMapPin, FiClock, FiTrendingUp, FiZap } from "react-icons/fi";
import {
  FaRunning,
  FaBicycle,
  FaSwimmer,
  FaHiking,
  FaSkiing,
  FaDumbbell
} from "react-icons/fa";
import {
  Card,
  CardHeader,
  UserAvatar,
  UserInfo,
  UserName,
  ActivityTime,
  ActivityType,
  ActivityTitle,
  ActivityDescription,
  StatsRow,
  StatItem,
  StatIcon,
  StatContent,
  StatLabel,
  StatValue,
  CardFooter,
  ActionButton,
  ActionsGroup,
} from "./ActivityCardStyles";
import ShareModal from "../ShareModal/ShareModal";

const activityIcons = {
  RUN: <FaRunning />,
  RIDE: <FaBicycle />,
  SWIM: <FaSwimmer />,
  HIKE: <FaHiking />,
  SKI: <FaSkiing />,
  WORKOUT: <FaDumbbell />,
};

const activityColors = {
  RUN: { bg: "#fff7ed", color: "#fc4c02" },
  RIDE: { bg: "#eff6ff", color: "#3b82f6" },
  SWIM: { bg: "#ecfeff", color: "#06b6d4" },
  HIKE: { bg: "#f0fdf4", color: "#22c55e" },
  SKI: { bg: "#faf5ff", color: "#a855f7" },
  WORKOUT: { bg: "#fdf2f8", color: "#ec4899" },
};

const formatDuration = (seconds) => {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  return `${minutes}m`;
};

const formatDistance = (km) => {
  return `${km.toFixed(2)} km`;
};

const getTimeAgo = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  if (diffDays < 30) return `${diffDays} days ago`;
  return date.toLocaleDateString();
};

const ActivityCard = ({ activity, onLike }) => {
  const [showShareModal, setShowShareModal] = useState(false);
  const colors = activityColors[activity.activityType] || activityColors.RUN;
  const icon = activityIcons[activity.activityType] || activityIcons.RUN;

  const handleLike = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (onLike) onLike(activity.id);
  };

  const handleShare = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setShowShareModal(true);
  };

  return (
    <>
      <Card to={`/activities/${activity.id}`}>
        <CardHeader>
          <UserAvatar>
            {activity.user?.avatarUrl ? (
              <img src={activity.user.avatarUrl} alt={activity.user.firstName} />
            ) : (
              activity.user?.firstName?.charAt(0) || "U"
            )}
          </UserAvatar>
          <UserInfo>
            <UserName>
              {activity.user?.firstName} {activity.user?.lastName}
            </UserName>
            <ActivityTime>{getTimeAgo(activity.createdAt)}</ActivityTime>
          </UserInfo>
          <ActivityType bgColor={colors.bg} color={colors.color}>
            {icon}
          </ActivityType>
        </CardHeader>

        <ActivityTitle>{activity.title}</ActivityTitle>
        {activity.description && (
          <ActivityDescription>{activity.description}</ActivityDescription>
        )}

        <StatsRow>
          <StatItem>
            <StatIcon>
              <FiMapPin />
            </StatIcon>
            <StatContent>
              <StatLabel>Distance</StatLabel>
              <StatValue>{formatDistance(activity.distance || 0)}</StatValue>
            </StatContent>
          </StatItem>
          <StatItem>
            <StatIcon>
              <FiClock />
            </StatIcon>
            <StatContent>
              <StatLabel>Time</StatLabel>
              <StatValue>{formatDuration(activity.duration || 0)}</StatValue>
            </StatContent>
          </StatItem>
          {activity.elevationGain > 0 && (
            <StatItem>
              <StatIcon>
                <FiTrendingUp />
              </StatIcon>
              <StatContent>
                <StatLabel>Elevation</StatLabel>
                <StatValue>{activity.elevationGain}m</StatValue>
              </StatContent>
            </StatItem>
          )}
          {activity.calories > 0 && (
            <StatItem>
              <StatIcon>
                <FiZap />
              </StatIcon>
              <StatContent>
                <StatLabel>Calories</StatLabel>
                <StatValue>{activity.calories}</StatValue>
              </StatContent>
            </StatItem>
          )}
        </StatsRow>

        <CardFooter>
          <ActionsGroup>
            <ActionButton onClick={handleLike} active={activity.likedByCurrentUser}>
              <FiHeart size={16} /> {activity.likesCount || 0}
            </ActionButton>
            <ActionButton>
              <FiMessageCircle size={16} /> {activity.commentsCount || 0}
            </ActionButton>
          </ActionsGroup>
          <ActionButton onClick={handleShare}>
            <FiShare2 size={16} />
          </ActionButton>
        </CardFooter>
      </Card>

      <ShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
        activityId={activity.id}
      />
    </>
  );
};

export default ActivityCard;
