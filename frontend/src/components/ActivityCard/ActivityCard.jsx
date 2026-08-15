import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
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
  MapPreview,
  CoverPhoto,
  MediaRow,
  MediaTile,
} from "./ActivityCardStyles";
import ShareModal from "../ShareModal/ShareModal";
import ActivityMap from "../ActivityMap/ActivityMap";
import { formatDuration, formatDistance, getTimeAgo } from "../../utils/formatters";
import { useUnits } from "../../context/UnitsContext";

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

const ActivityCard = ({ activity, onLike }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const unitsCtx = useUnits();
  const [showShareModal, setShowShareModal] = useState(false);
  const colors = activityColors[activity.activityType] || activityColors.RUN;
  const icon = activityIcons[activity.activityType] || activityIcons.RUN;
  const coverPhotoUrl =
    activity.photos && activity.photos.length > 0
      ? activity.photos[0].startsWith("http")
        ? activity.photos[0]
        : `${process.env.REACT_APP_API_BASE_URL || "http://localhost:8080"}${activity.photos[0]}`
      : null;

  const handleLike = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (onLike) onLike(activity.id, activity.likedByCurrentUser);
  };

  const handleShare = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setShowShareModal(true);
  };

  const handleUserClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (activity.user?.id) {
      navigate(`/profile/${activity.user.id}`);
    }
  };

  return (
    <>
      <Card to={`/activities/${activity.id}`}>
        <CardHeader>
          <UserAvatar onClick={handleUserClick} style={{ cursor: "pointer" }}>
            {activity.user?.avatar ? (
              <img src={activity.user.avatar} alt={activity.user.firstName} />
            ) : (
              activity.user?.firstName?.charAt(0) || "U"
            )}
          </UserAvatar>
          <UserInfo>
            <UserName onClick={handleUserClick} style={{ cursor: "pointer" }}>
              {activity.user?.firstName} {activity.user?.lastName}
            </UserName>
            <ActivityTime>{getTimeAgo(activity.startTime || activity.createdAt)}</ActivityTime>
          </UserInfo>
          <ActivityType bgColor={colors.bg} color={colors.color}>
            {icon}
          </ActivityType>
        </CardHeader>

        <ActivityTitle>{activity.title}</ActivityTitle>
        {activity.description && (
          <ActivityDescription>{activity.description}</ActivityDescription>
        )}

        {coverPhotoUrl && activity.routePolyline ? (
          // Strava-style media row: route map square beside the photo square.
          <MediaRow onClick={(e) => e.preventDefault()}>
            <MediaTile>
              <ActivityMap
                coordinates={activity.routePolyline}
                height="100%"
                showMarkers={false}
              />
            </MediaTile>
            <MediaTile>
              <img src={coverPhotoUrl} alt={activity.title} />
            </MediaTile>
          </MediaRow>
        ) : coverPhotoUrl ? (
          <CoverPhoto>
            <img src={coverPhotoUrl} alt={activity.title} />
          </CoverPhoto>
        ) : activity.routePolyline ? (
          <MapPreview onClick={(e) => e.preventDefault()}>
            <ActivityMap
              coordinates={activity.routePolyline}
              height="160px"
              showMarkers={false}
            />
          </MapPreview>
        ) : null}

        <StatsRow>
          <StatItem>
            <StatIcon>
              <FiMapPin />
            </StatIcon>
            <StatContent>
              <StatLabel>{t("activities.stats.distance")}</StatLabel>
              <StatValue>{formatDistance(activity.distance || 0, unitsCtx)}</StatValue>
            </StatContent>
          </StatItem>
          <StatItem>
            <StatIcon>
              <FiClock />
            </StatIcon>
            <StatContent>
              <StatLabel>{t("activities.stats.time")}</StatLabel>
              <StatValue>{formatDuration(activity.duration || 0)}</StatValue>
            </StatContent>
          </StatItem>
          {activity.elevationGain > 0 && (
            <StatItem>
              <StatIcon>
                <FiTrendingUp />
              </StatIcon>
              <StatContent>
                <StatLabel>{t("activities.stats.elevation")}</StatLabel>
                <StatValue>{Math.round(unitsCtx.convertElevation(activity.elevationGain))}{unitsCtx.elevationUnit}</StatValue>
              </StatContent>
            </StatItem>
          )}
          {activity.calories > 0 && (
            <StatItem>
              <StatIcon>
                <FiZap />
              </StatIcon>
              <StatContent>
                <StatLabel>{t("activities.stats.calories")}</StatLabel>
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

export default React.memo(ActivityCard);
