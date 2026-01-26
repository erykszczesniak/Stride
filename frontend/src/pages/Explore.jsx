import React, { useEffect, useState } from "react";
import { FiSearch, FiFilter, FiUsers, FiGlobe, FiTrendingUp, FiHeart, FiMessageCircle, FiShare2 } from "react-icons/fi";
import { FaFire, FaRunning, FaBicycle, FaSwimmer, FaDumbbell } from "react-icons/fa";
import { Link } from "react-router-dom";
import Layout from "../components/Layout/Layout";
import ActivityService from "../api/ActivityService";
import UserService from "../api/UserService";
import {
  ExploreContainer,
  SearchSection,
  SearchWrapper,
  SearchInput,
  SearchIcon,
  FilterButton,
  TabsContainer,
  Tab,
  TabIcon,
  MainContent,
  FeedColumn,
  SidebarColumn,
  ActivityFeed,
  ActivityCard,
  CardHeader,
  UserAvatar,
  UserInfo,
  UserName,
  ActivityTime,
  ActivityTypeBadge,
  ActivityTitle,
  ActivityDescription,
  StatsRow,
  StatItem,
  StatIcon,
  StatValue,
  StatLabel,
  CardFooter,
  ActionButton,
  ActionsGroup,
  SidebarCard,
  SidebarTitle,
  AthletesList,
  AthleteItem,
  AthleteAvatar,
  AthleteInfo,
  AthleteName,
  AthleteStats,
  FollowButton,
  SeeMoreLink,
  ChallengeCard,
  ChallengeIcon,
  ChallengeTitle,
  ChallengeDescription,
  ProgressSection,
  ProgressLabel,
  ProgressValue,
  ProgressBar,
  ProgressFill,
  ParticipantsCount,
  LoadingSpinner,
  EmptyState,
  EmptyIcon,
  EmptyText,
} from "./ExploreStyles";

const activityIcons = {
  RUN: <FaRunning />,
  RIDE: <FaBicycle />,
  SWIM: <FaSwimmer />,
  WORKOUT: <FaDumbbell />,
};

const activityColors = {
  RUN: { bg: "#fff3e0", color: "#fc4c02" },
  RIDE: { bg: "#e3f2fd", color: "#1976d2" },
  SWIM: { bg: "#e0f7fa", color: "#00838f" },
  WORKOUT: { bg: "#fce4ec", color: "#c2185b" },
};

const formatDuration = (seconds) => {
  if (!seconds) return "0m";
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  return `${minutes}m`;
};

const getTimeAgo = (dateString) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  if (diffMins < 60) return `${diffMins} minutes ago`;
  if (diffHours < 24) return `${diffHours} hours ago`;
  if (diffDays < 7) return `${diffDays} days ago`;
  return date.toLocaleDateString();
};

const calculateCalories = (activityType, duration, distance) => {
  // Simple calorie estimation based on activity type
  const caloriesPerMinute = {
    RUN: 10,
    RIDE: 8,
    SWIM: 9,
    WORKOUT: 7,
  };
  const minutes = duration / 60;
  return Math.round(minutes * (caloriesPerMinute[activityType] || 7));
};

const Explore = () => {
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("discover");
  const [activities, setActivities] = useState([]);
  const [suggestedAthletes, setSuggestedAthletes] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [likedActivities, setLikedActivities] = useState(new Set());

  const userId = 1; // Hardcoded for now

  useEffect(() => {
    const fetchExploreData = async () => {
      try {
        setLoading(true);

        const [activitiesRes, suggestedRes] = await Promise.all([
          ActivityService.getPublicActivities(0, 20).catch(() => ({ data: [] })),
          UserService.getSuggestedUsers(userId, 5).catch(() => ({ data: [] })),
        ]);

        setActivities(Array.isArray(activitiesRes.data) ? activitiesRes.data : []);
        setSuggestedAthletes(Array.isArray(suggestedRes.data) ? suggestedRes.data : []);
      } catch (error) {
        console.error("Error fetching explore data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchExploreData();
  }, [activeTab]);

  const handleLike = async (activityId) => {
    try {
      if (likedActivities.has(activityId)) {
        await ActivityService.unlikeActivity(activityId, userId);
        setLikedActivities(prev => {
          const newSet = new Set(prev);
          newSet.delete(activityId);
          return newSet;
        });
      } else {
        await ActivityService.likeActivity(activityId, userId);
        setLikedActivities(prev => new Set([...prev, activityId]));
      }
    } catch (error) {
      console.error("Error toggling like:", error);
    }
  };

  const handleFollow = async (athleteId) => {
    try {
      await UserService.followUser(userId, athleteId);
      // Update UI to show followed state
    } catch (error) {
      console.error("Error following user:", error);
    }
  };

  const tabs = [
    { id: "following", label: "Following", icon: <FiUsers /> },
    { id: "discover", label: "Discover", icon: <FiGlobe /> },
    { id: "trending", label: "Trending", icon: <FiTrendingUp /> },
  ];

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <ExploreContainer>
        <SearchSection>
          <SearchWrapper>
            <SearchIcon>
              <FiSearch size={18} />
            </SearchIcon>
            <SearchInput
              type="text"
              placeholder="Search athletes, activities, clubs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </SearchWrapper>
          <FilterButton>
            <FiFilter size={18} />
          </FilterButton>
        </SearchSection>

        <TabsContainer>
          {tabs.map((tab) => (
            <Tab
              key={tab.id}
              active={activeTab === tab.id}
              onClick={() => setActiveTab(tab.id)}
            >
              <TabIcon>{tab.icon}</TabIcon>
              {tab.label}
            </Tab>
          ))}
        </TabsContainer>

        <MainContent>
          <FeedColumn>
            {activities.length > 0 ? (
              <ActivityFeed>
                {activities.map((activity) => {
                  const colors = activityColors[activity.activityType] || activityColors.RUN;
                  const icon = activityIcons[activity.activityType] || activityIcons.RUN;
                  const isLiked = likedActivities.has(activity.id) || activity.likedByCurrentUser;
                  const calories = calculateCalories(activity.activityType, activity.duration, activity.distance);

                  return (
                    <ActivityCard key={activity.id}>
                      <CardHeader>
                        <UserAvatar as={Link} to={`/profile/${activity.user?.id}`}>
                          {activity.user?.avatarUrl ? (
                            <img src={activity.user.avatarUrl} alt={activity.user.firstName} />
                          ) : (
                            activity.user?.firstName?.charAt(0) || "U"
                          )}
                        </UserAvatar>
                        <UserInfo>
                          <UserName as={Link} to={`/profile/${activity.user?.id}`}>
                            {activity.user?.firstName} {activity.user?.lastName}
                          </UserName>
                          <ActivityTime>{getTimeAgo(activity.createdAt)}</ActivityTime>
                        </UserInfo>
                        <ActivityTypeBadge bgColor={colors.bg} color={colors.color}>
                          {icon}
                        </ActivityTypeBadge>
                      </CardHeader>

                      <ActivityTitle
                        as={Link}
                        to={`/activities/${activity.id}`}
                        titleColor={activity.activityType === "RUN" ? "#fc4c02" : "#333"}
                      >
                        {activity.title}
                      </ActivityTitle>

                      {activity.description && (
                        <ActivityDescription>{activity.description}</ActivityDescription>
                      )}

                      <StatsRow>
                        <StatItem>
                          <StatIcon>📍</StatIcon>
                          <StatValue>{(activity.distance || 0).toFixed(1)}</StatValue>
                          <StatLabel>km</StatLabel>
                        </StatItem>
                        <StatItem>
                          <StatIcon>⏱️</StatIcon>
                          <StatValue>{formatDuration(activity.duration)}</StatValue>
                          <StatLabel>time</StatLabel>
                        </StatItem>
                        <StatItem>
                          <StatIcon>⛰️</StatIcon>
                          <StatValue>{activity.elevationGain || 0}</StatValue>
                          <StatLabel>m elev</StatLabel>
                        </StatItem>
                        <StatItem>
                          <StatIcon>🔥</StatIcon>
                          <StatValue>{calories}</StatValue>
                          <StatLabel>cal</StatLabel>
                        </StatItem>
                      </StatsRow>

                      <CardFooter>
                        <ActionsGroup>
                          <ActionButton
                            onClick={() => handleLike(activity.id)}
                            active={isLiked}
                          >
                            <FiHeart size={16} /> {(activity.likesCount || 0) + (likedActivities.has(activity.id) ? 1 : 0)}
                          </ActionButton>
                          <ActionButton as={Link} to={`/activities/${activity.id}`}>
                            <FiMessageCircle size={16} /> {activity.commentsCount || 0}
                          </ActionButton>
                        </ActionsGroup>
                        <ActionButton>
                          <FiShare2 size={16} />
                        </ActionButton>
                      </CardFooter>
                    </ActivityCard>
                  );
                })}
              </ActivityFeed>
            ) : (
              <EmptyState>
                <EmptyIcon>🔍</EmptyIcon>
                <EmptyText>No activities found. Check back later for new content!</EmptyText>
              </EmptyState>
            )}
          </FeedColumn>

          <SidebarColumn>
            <SidebarCard>
              <SidebarTitle>Athletes to Follow</SidebarTitle>
              <AthletesList>
                {suggestedAthletes.map((athlete) => (
                  <AthleteItem key={athlete.id}>
                    <AthleteAvatar as={Link} to={`/profile/${athlete.id}`}>
                      {athlete.avatar ? (
                        <img src={athlete.avatar} alt={athlete.firstName} />
                      ) : (
                        athlete.firstName?.charAt(0)
                      )}
                    </AthleteAvatar>
                    <AthleteInfo>
                      <AthleteName as={Link} to={`/profile/${athlete.id}`}>
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
              <SeeMoreLink to="/athletes">See More</SeeMoreLink>
            </SidebarCard>

            <ChallengeCard>
              <ChallengeIcon>
                <FaFire />
              </ChallengeIcon>
              <ChallengeTitle>January Challenge</ChallengeTitle>
              <ChallengeDescription>
                Run 100km this month and earn the exclusive badge!
              </ChallengeDescription>
              <ProgressSection>
                <ProgressLabel>Your progress</ProgressLabel>
                <ProgressValue>67.3 / 100 km</ProgressValue>
              </ProgressSection>
              <ProgressBar>
                <ProgressFill progress={67.3} />
              </ProgressBar>
              <ParticipantsCount>12,453 participants</ParticipantsCount>
            </ChallengeCard>
          </SidebarColumn>
        </MainContent>
      </ExploreContainer>
    </Layout>
  );
};

export default Explore;
