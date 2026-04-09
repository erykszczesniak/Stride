import { useState, useEffect, useMemo, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useTheme } from "styled-components";
import ActivityService from "../api/ActivityService";
import UserService from "../api/UserService";
import Layout from "../components/Layout/Layout";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import {
  Container,
  ProfileHeader,
  CoverImage,
  ProfileContent,
  Avatar,
  ProfileInfo,
  ProfileName,
  ProfileBio,
  ProfileLocation,
  ProfileStats,
  StatItem,
  StatValue,
  StatLabel,
  EditButton,
  TabsContainer,
  Tab,
  TabContent,
  StatsGrid,
  StatCard,
  StatIcon,
  SectionTitle,
  MonthlyChart,
  MonthBar,
  MonthLabel,
  MonthValue,
  AchievementsGrid,
  LoadingSpinner,
  EmptyState,
  FollowButton,
  ShareButton,
  ProfileActions,
} from "./ProfileStyles";

const allAchievements = [
  {
    icon: "\u{1F451}",
    title: "100km Club",
    description: "Ran 100km in a month",
    color: "#FFD700",
    check: (totals) => totals.distance >= 100,
  },
  {
    icon: "\u{26F0}\uFE0F",
    title: "Mountain Goat",
    description: "5,000m elevation gained",
    color: "#4CAF50",
    check: (totals) => totals.elevation >= 5000,
  },
  {
    icon: "\u{1F525}",
    title: "Calorie Crusher",
    description: "Burned 10,000 calories",
    color: "#FF6B35",
    check: (totals) => totals.calories >= 10000,
  },
  {
    icon: "\u26A1",
    title: "Speed Demon",
    description: "Avg speed over 12 km/h",
    color: "#9C27B0",
    check: (totals) => totals.count > 0 && totals.avgSpeed > 12,
  },
  {
    icon: "\u2B50",
    title: "Rising Star",
    description: "50 activities completed",
    color: "#2196F3",
    check: (totals) => totals.count >= 50,
  },
  {
    icon: "\u{1F3C5}",
    title: "Consistent",
    description: "10+ activities completed",
    color: "#C0C0C0",
    check: (totals) => totals.count >= 10,
  },
];

const Profile = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const theme = useTheme();
  const [user, setUser] = useState(null);
  const [activities, setActivities] = useState([]);
  const [stats, setStats] = useState(null);
  const [activeTab, setActiveTab] = useState("overview");
  const [loading, setLoading] = useState(true);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [isFollowed, setIsFollowed] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);
  const [shareTooltip, setShareTooltip] = useState(false);

  useEffect(() => {
    fetchProfileData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const fetchProfileData = async () => {
    try {
      setLoading(true);

      if (id) {
        // Check if this is the current user's profile viewed via /profile/:id
        let currentUser;
        try {
          currentUser = await UserService.getCurrentUser();
        } catch (err) {
          // ignore
        }

        const isOwn = currentUser?.data?.id?.toString() === id.toString();
        setIsOwnProfile(isOwn);

        if (!isOwn) {
          // Fetch public profile via the dedicated endpoint
          const profileData = await UserService.getUserProfile(id);
          const profile = profileData.data;

          setUser({
            id: profile.id,
            firstName: profile.firstName,
            lastName: profile.lastName,
            avatar: profile.avatar,
            bio: profile.bio,
            city: profile.city,
            country: profile.country,
            joinedDate: new Date(profile.createdAt).getFullYear(),
          });

          setFollowersCount(profile.followersCount);
          setFollowingCount(profile.followingCount);
          setIsFollowed(profile.followedByCurrentUser);
          setActivities(profile.recentActivities || []);
          setStats(null);
          setLoading(false);
          return;
        }

        // It's own profile viewed via /profile/:id, fall through to own-profile logic
      } else {
        setIsOwnProfile(true);
      }

      // Own profile flow
      const userData = await UserService.getCurrentUser();
      const userInfo = userData.data;

      setUser({
        id: userInfo.id,
        firstName: userInfo.firstName,
        lastName: userInfo.lastName,
        email: userInfo.email,
        avatar: userInfo.avatar,
        bio: userInfo.bio,
        city: userInfo.city,
        country: userInfo.country,
        joinedDate: new Date(userInfo.createdAt).getFullYear(),
      });

      const userId = userInfo.id;

      // Fetch user activities
      const activitiesData = await ActivityService.getUserActivities(userId, 0, 10);
      setActivities(activitiesData.data || []);

      // Fetch monthly stats
      const monthlyData = await ActivityService.getMonthlyStats();
      setStats(monthlyData.data);

      // Fetch followers and following count
      try {
        const followersData = await UserService.getFollowers(userId);
        setFollowersCount(followersData.data?.length || 0);
      } catch (err) {
      }

      try {
        const followingData = await UserService.getFollowing(userId);
        setFollowingCount(followingData.data?.length || 0);
      } catch (err) {
      }

      setLoading(false);
    } catch (error) {
      setLoading(false);
    }
  };

  const handleFollowToggle = async () => {
    if (followLoading || !user) return;
    setFollowLoading(true);

    // Optimistic update
    const wasFollowed = isFollowed;
    setIsFollowed(!wasFollowed);
    setFollowersCount((prev) => (wasFollowed ? prev - 1 : prev + 1));

    try {
      if (wasFollowed) {
        await UserService.unfollowUser(user.id);
      } else {
        await UserService.followUser(user.id);
      }
    } catch (error) {
      // Revert on error
      setIsFollowed(wasFollowed);
      setFollowersCount((prev) => (wasFollowed ? prev + 1 : prev - 1));
    } finally {
      setFollowLoading(false);
    }
  };

  const handleShareProfile = () => {
    const profileUrl = `${window.location.origin}/profile/${user?.id}`;
    navigator.clipboard.writeText(profileUrl).then(() => {
      setShareTooltip(true);
      setTimeout(() => setShareTooltip(false), 2000);
    });
  };

  const totals = useMemo(() => {
    if (!activities.length)
      return { distance: 0, time: 0, elevation: 0, count: 0, calories: 0, avgSpeed: 0 };

    const distance = activities.reduce((sum, a) => sum + (a.distance || 0), 0);
    const time = activities.reduce((sum, a) => sum + (a.duration || 0), 0);
    const elevation = activities.reduce((sum, a) => sum + (a.elevationGain || 0), 0);
    const calories = activities.reduce((sum, a) => sum + (a.calories || 0), 0);
    const totalSpeed = activities.reduce((sum, a) => sum + (a.averageSpeed || 0), 0);

    return {
      distance,
      time,
      elevation,
      count: activities.length,
      calories,
      avgSpeed: activities.length > 0 ? totalSpeed / activities.length : 0,
    };
  }, [activities]);

  const earnedAchievements = useMemo(() => {
    return allAchievements.filter((a) => a.check(totals));
  }, [totals]);

  const monthlyProgress = stats?.monthlyStats || [];

  const maxDistance = monthlyProgress.length > 0 ? Math.max(...monthlyProgress.map((m) => m.distance)) : 1;

  const handleLikeActivity = useCallback(async (activityId, isLiked) => {
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
      // silently handle error
    }
  }, []);

  if (loading) {
    return (
      <Layout>
        <Container>
          <LoadingSpinner>Loading profile...</LoadingSpinner>
        </Container>
      </Layout>
    );
  }

  if (!user) {
    return (
      <Layout>
        <Container>
          <EmptyState>User not found</EmptyState>
        </Container>
      </Layout>
    );
  }

  return (
    <Layout>
      <ProfileHeader>
        <CoverImage />
        <ProfileContent>
          <Avatar>
            {user.avatar ? (
              <img src={user.avatar} alt={user.firstName} />
            ) : (
              <span>
                {user.firstName?.[0]}
                {user.lastName?.[0]}
              </span>
            )}
          </Avatar>
          <ProfileInfo>
            <ProfileName>
              {user.firstName} {user.lastName}
            </ProfileName>
            {user.bio && <ProfileBio>{"\u{1F3C6}"} {user.bio}</ProfileBio>}
            {(user.city || user.country) && (
              <ProfileLocation>
                {"\u{1F4CD}"} {user.city}, {user.country}
              </ProfileLocation>
            )}
            <ProfileLocation>{"\u{1F4C5}"} Joined {user.joinedDate}</ProfileLocation>
          </ProfileInfo>
          <ProfileActions>
            {isOwnProfile ? (
              <>
                <EditButton onClick={() => navigate('/settings')}>{"\u2699\uFE0F"} Edit Profile</EditButton>
                <ShareButton onClick={handleShareProfile}>
                  {shareTooltip ? "Copied!" : "\u{1F517} Share Profile"}
                </ShareButton>
              </>
            ) : (
              <FollowButton $followed={isFollowed} onClick={handleFollowToggle} disabled={followLoading}>
                {isFollowed ? "Following" : "Follow"}
              </FollowButton>
            )}
          </ProfileActions>
        </ProfileContent>

        <ProfileStats>
          <StatItem>
            <StatValue>{activities.length}</StatValue>
            <StatLabel>Activities</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{followersCount.toLocaleString()}</StatValue>
            <StatLabel>Followers</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{followingCount.toLocaleString()}</StatValue>
            <StatLabel>Following</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>
              {activities
                .reduce((sum, a) => sum + (a.likesCount || 0), 0)
                .toLocaleString()}
            </StatValue>
            <StatLabel>Kudos</StatLabel>
          </StatItem>
        </ProfileStats>
      </ProfileHeader>

      <Container>
        {isOwnProfile ? (
          <>
            <TabsContainer>
              <Tab
                active={activeTab === "overview"}
                onClick={() => setActiveTab("overview")}
              >
                Overview
              </Tab>
              <Tab
                active={activeTab === "activities"}
                onClick={() => setActiveTab("activities")}
              >
                Activities
              </Tab>
              <Tab
                active={activeTab === "achievements"}
                onClick={() => setActiveTab("achievements")}
              >
                Achievements
              </Tab>
            </TabsContainer>

            {activeTab === "overview" && (
              <TabContent>
                <SectionTitle>{"\u{1F3C6}"} All-Time Stats</SectionTitle>
                <StatsGrid>
                  <StatCard color="#fc5200">
                    <StatIcon>{"\u{1F3C3}"}</StatIcon>
                    <StatValue>{totals.distance.toFixed(0)}</StatValue>
                    <StatLabel>Total km</StatLabel>
                  </StatCard>
                  <StatCard color="#0095ff">
                    <StatIcon>{"\u{1F4C5}"}</StatIcon>
                    <StatValue>{Math.floor(totals.time / 3600)}</StatValue>
                    <StatLabel>Hours Active</StatLabel>
                  </StatCard>
                  <StatCard color="#4caf50">
                    <StatIcon>{"\u{26F0}\uFE0F"}</StatIcon>
                    <StatValue>{totals.elevation.toLocaleString()}</StatValue>
                    <StatLabel>m Elevation</StatLabel>
                  </StatCard>
                  <StatCard color="#9c27b0">
                    <StatIcon>{"\u26A1"}</StatIcon>
                    <StatValue>{totals.count}</StatValue>
                    <StatLabel>Activities</StatLabel>
                  </StatCard>
                </StatsGrid>

                {monthlyProgress.length > 0 && (
                  <>
                    <SectionTitle>{"\u{1F4CA}"} Monthly Progress</SectionTitle>
                    <MonthlyChart>
                      {monthlyProgress.map((stat) => (
                        <div key={stat.month}>
                          <MonthBar height={(stat.distance / maxDistance) * 100}>
                            <div />
                          </MonthBar>
                          <MonthLabel>{stat.month}</MonthLabel>
                          <MonthValue>{stat.distance}</MonthValue>
                        </div>
                      ))}
                    </MonthlyChart>
                  </>
                )}

                <SectionTitle>{"\u{1F3C5}"} Achievements</SectionTitle>
                {earnedAchievements.length === 0 ? (
                  <EmptyState>No achievements earned yet. Keep training!</EmptyState>
                ) : (
                <AchievementsGrid>
                  {earnedAchievements.map((achievement, index) => (
                    <div key={index} style={{ display: "flex" }}>
                      <div
                        style={{
                          flex: 1,
                          background: `${achievement.color}20`,
                          border: `2px solid ${achievement.color}`,
                          borderRadius: "16px",
                          padding: "24px",
                          textAlign: "center",
                          cursor: "pointer",
                          transition: "transform 0.2s",
                        }}
                        onMouseEnter={(e) =>
                          (e.currentTarget.style.transform = "translateY(-4px)")
                        }
                        onMouseLeave={(e) =>
                          (e.currentTarget.style.transform = "translateY(0)")
                        }
                      >
                        <div style={{ fontSize: "48px", marginBottom: "12px" }}>
                          {achievement.icon}
                        </div>
                        <div
                          style={{
                            fontWeight: "700",
                            fontSize: "14px",
                            marginBottom: "4px",
                            color: theme.textPrimary,
                          }}
                        >
                          {achievement.title}
                        </div>
                        <div style={{ fontSize: "12px", color: theme.textSecondary }}>
                          {achievement.description}
                        </div>
                      </div>
                    </div>
                  ))}
                </AchievementsGrid>
                )}
              </TabContent>
            )}

            {activeTab === "activities" && (
              <TabContent>
                {activities.length > 0 ? (
                  <div
                    style={{
                      display: "flex",
                      flexDirection: "column",
                      gap: "16px",
                    }}
                  >
                    {activities.map((activity) => (
                      <ActivityCard
                        key={activity.id}
                        activity={activity}
                        onLike={handleLikeActivity}
                      />
                    ))}
                  </div>
                ) : (
                  <EmptyState>No activities yet</EmptyState>
                )}
              </TabContent>
            )}

            {activeTab === "achievements" && (
              <TabContent>
                <div
                  style={{
                    background: theme.bgCard,
                    borderRadius: "16px",
                    padding: "24px",
                    marginBottom: "24px",
                    border: `1px solid ${theme.border}`,
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                    }}
                  >
                    <div>
                      <h3
                        style={{
                          fontSize: "18px",
                          fontWeight: "700",
                          marginBottom: "8px",
                          color: theme.textPrimary,
                        }}
                      >
                        Achievement Progress
                      </h3>
                      <p style={{ fontSize: "14px", color: theme.textSecondary }}>
                        You've earned {earnedAchievements.length} of {allAchievements.length} badges
                      </p>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <div
                        style={{
                          fontSize: "32px",
                          fontWeight: "900",
                          color: "#fc5200",
                        }}
                      >
                        {allAchievements.length > 0 ? Math.round((earnedAchievements.length / allAchievements.length) * 100) : 0}%
                      </div>
                      <div style={{ fontSize: "14px", color: theme.textSecondary }}>
                        Complete
                      </div>
                    </div>
                  </div>
                  <div
                    style={{
                      height: "12px",
                      background: theme.bgSecondary,
                      borderRadius: "6px",
                      marginTop: "16px",
                      overflow: "hidden",
                    }}
                  >
                    <div
                      style={{
                        height: "100%",
                        width: `${allAchievements.length > 0 ? Math.round((earnedAchievements.length / allAchievements.length) * 100) : 0}%`,
                        background: "linear-gradient(90deg, #fc5200, #ff6b35)",
                        borderRadius: "6px",
                      }}
                    />
                  </div>
                </div>

                <AchievementsGrid>
                  {allAchievements.map((achievement, index) => {
                    const isEarned = achievement.check(totals);
                    return (
                      <div key={index} style={{ display: "flex" }}>
                        <div
                          style={{
                            flex: 1,
                            background: isEarned ? `${achievement.color}20` : theme.bgSecondary,
                            border: `2px solid ${isEarned ? achievement.color : theme.border}`,
                            borderRadius: "16px",
                            padding: "24px",
                            textAlign: "center",
                            cursor: "pointer",
                            transition: "transform 0.2s",
                            opacity: isEarned ? 1 : 0.5,
                          }}
                          onMouseEnter={(e) =>
                            (e.currentTarget.style.transform = "translateY(-4px)")
                          }
                          onMouseLeave={(e) =>
                            (e.currentTarget.style.transform = "translateY(0)")
                          }
                        >
                          <div style={{ fontSize: "48px", marginBottom: "12px" }}>
                            {achievement.icon}
                          </div>
                          <div
                            style={{
                              fontWeight: "700",
                              fontSize: "14px",
                              marginBottom: "4px",
                              color: theme.textPrimary,
                            }}
                          >
                            {achievement.title}
                          </div>
                          <div style={{ fontSize: "12px", color: theme.textSecondary }}>
                            {achievement.description}
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </AchievementsGrid>
              </TabContent>
            )}
          </>
        ) : (
          /* Public profile view - other user */
          <TabContent>
            <SectionTitle>Recent Activities</SectionTitle>
            {activities.length > 0 ? (
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "16px",
                }}
              >
                {activities.map((activity) => (
                  <ActivityCard
                    key={activity.id}
                    activity={activity}
                    onLike={handleLikeActivity}
                  />
                ))}
              </div>
            ) : (
              <EmptyState>No public activities yet</EmptyState>
            )}
          </TabContent>
        )}
      </Container>
    </Layout>
  );
};

export default Profile;
