import React, { useState } from "react";
import { FiHeart, FiMessageCircle, FiShare2 } from "react-icons/fi";
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
  StatsRow,
  StatItem,
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
  RUN: { bg: "#fff3e0", color: "#fc4c02" },
  RIDE: { bg: "#e3f2fd", color: "#1976d2" },
  SWIM: { bg: "#e0f7fa", color: "#00838f" },
  HIKE: { bg: "#e8f5e9", color: "#388e3c" },
  SKI: { bg: "#f3e5f5", color: "#7b1fa2" },
  WORKOUT: { bg: "#fce4ec", color: "#c2185b" },
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

const formatPace = (seconds, distance) => {
  if (!distance || distance === 0) return "-";
  const paceSeconds = seconds / distance;
  const mins = Math.floor(paceSeconds / 60);
  const secs = Math.floor(paceSeconds % 60);
  return `${mins}:${secs.toString().padStart(2, "0")} /km`;
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
            {icon} {activity.activityType}
          </ActivityType>
        </CardHeader>

        <ActivityTitle>{activity.title}</ActivityTitle>

        <StatsRow>
          <StatItem>
            <StatLabel>Distance</StatLabel>
            <StatValue>{formatDistance(activity.distance || 0)}</StatValue>
          </StatItem>
          <StatItem>
            <StatLabel>Duration</StatLabel>
            <StatValue>{formatDuration(activity.duration || 0)}</StatValue>
          </StatItem>
          <StatItem>
            <StatLabel>Pace</StatLabel>
            <StatValue>{formatPace(activity.duration, activity.distance)}</StatValue>
          </StatItem>
          {activity.elevationGain > 0 && (
            <StatItem>
              <StatLabel>Elevation</StatLabel>
              <StatValue>{activity.elevationGain}m</StatValue>
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
