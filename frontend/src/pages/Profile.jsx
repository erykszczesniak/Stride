import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ActivityService from "../api/ActivityService";
import UserService from "../api/UserService";
import Layout from "../components/Layout/Layout";
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
} from "./ProfileStyles";

const Profile = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [activities, setActivities] = useState([]);
  const [stats, setStats] = useState(null);
  const [activeTab, setActiveTab] = useState("overview");
  const [loading, setLoading] = useState(true);
  const [isOwnProfile, setIsOwnProfile] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);

  useEffect(() => {
    fetchProfileData();
  }, [id]);

  const fetchProfileData = async () => {
    try {
      setLoading(true);

      // If no ID provided, fetch current user's profile
      setIsOwnProfile(!id);

      // Fetch user data from backend
      const userData = id
        ? await UserService.getUserById(id)
        : await UserService.getCurrentUser();
      const userInfo = userData.data;

      // Format user data
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

      // Use the user ID from the response for fetching related data
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
        console.error("Error fetching followers:", err);
      }

      try {
        const followingData = await UserService.getFollowing(userId);
        setFollowingCount(followingData.data?.length || 0);
      } catch (err) {
        console.error("Error fetching following:", err);
      }

      setLoading(false);
    } catch (error) {
      console.error("Error fetching profile:", error);
      setLoading(false);
    }
  };

  const calculateTotals = () => {
    if (!activities.length)
      return { distance: 0, time: 0, elevation: 0, count: 0 };

    return {
      distance: activities.reduce((sum, a) => sum + (a.distance || 0), 0),
      time: activities.reduce((sum, a) => sum + (a.duration || 0), 0),
      elevation: activities.reduce((sum, a) => sum + (a.elevationGain || 0), 0),
      count: activities.length,
    };
  };

  const totals = calculateTotals();

  const achievements = [
    {
      icon: "👑",
      title: "100km Club",
      description: "Ran 100km in a month",
      color: "#FFD700",
    },
    {
      icon: "⛰️",
      title: "Mountain Goat",
      description: "5,000m elevation gained",
      color: "#4CAF50",
    },
    {
      icon: "🔥",
      title: "Calorie Crusher",
      description: "Burned 10,000 calories",
      color: "#FF6B35",
    },
    {
      icon: "⚡",
      title: "Speed Demon",
      description: "Fastest 5km time",
      color: "#9C27B0",
    },
    {
      icon: "⭐",
      title: "Rising Star",
      description: "50 activities completed",
      color: "#2196F3",
    },
    {
      icon: "🏅",
      title: "Consistent",
      description: "30 day streak",
      color: "#C0C0C0",
    },
  ];

  const monthlyProgress = stats?.monthlyStats || [
    { month: "Jan", distance: 142 },
    { month: "Feb", distance: 168 },
    { month: "Mar", distance: 195 },
    { month: "Apr", distance: 178 },
    { month: "May", distance: 203 },
    { month: "Jun", distance: 156 },
  ];

  const maxDistance = Math.max(...monthlyProgress.map((m) => m.distance));

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
            {user.bio && <ProfileBio>🏆 {user.bio}</ProfileBio>}
            {(user.city || user.country) && (
              <ProfileLocation>
                📍 {user.city}, {user.country}
              </ProfileLocation>
            )}
            <ProfileLocation>📅 Joined {user.joinedDate}</ProfileLocation>
          </ProfileInfo>
          {isOwnProfile && <EditButton onClick={() => navigate('/settings')}>⚙️ Edit Profile</EditButton>}
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
                .reduce((sum, a) => sum + (a.kudosCount || 0), 0)
                .toLocaleString()}
            </StatValue>
            <StatLabel>Kudos</StatLabel>
          </StatItem>
        </ProfileStats>
      </ProfileHeader>

      <Container>
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
            <SectionTitle>🏆 All-Time Stats</SectionTitle>
            <StatsGrid>
              <StatCard color="#fc5200">
                <StatIcon>🏃</StatIcon>
                <StatValue>{totals.distance.toFixed(0)}</StatValue>
                <StatLabel>Total km</StatLabel>
              </StatCard>
              <StatCard color="#0095ff">
                <StatIcon>📅</StatIcon>
                <StatValue>{Math.floor(totals.time / 60)}</StatValue>
                <StatLabel>Hours Active</StatLabel>
              </StatCard>
              <StatCard color="#4caf50">
                <StatIcon>⛰️</StatIcon>
                <StatValue>{totals.elevation.toLocaleString()}</StatValue>
                <StatLabel>m Elevation</StatLabel>
              </StatCard>
              <StatCard color="#9c27b0">
                <StatIcon>⚡</StatIcon>
                <StatValue>{totals.count}</StatValue>
                <StatLabel>Activities</StatLabel>
              </StatCard>
            </StatsGrid>

            <SectionTitle>📊 Monthly Progress</SectionTitle>
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

            <SectionTitle>🏅 Recent Achievements</SectionTitle>
            <AchievementsGrid>
              {achievements.map((achievement, index) => (
                <div key={index}>
                  <div
                    style={{
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
                      }}
                    >
                      {achievement.title}
                    </div>
                    <div style={{ fontSize: "12px", color: "#666" }}>
                      {achievement.description}
                    </div>
                  </div>
                </div>
              ))}
            </AchievementsGrid>
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
                  <div
                    key={activity.id}
                    style={{
                      background: "white",
                      borderRadius: "16px",
                      padding: "24px",
                      border: "1px solid #e5e5e5",
                    }}
                  >
                    <h3>{activity.title}</h3>
                    <p>Distance: {activity.distance} km</p>
                    <p>Duration: {activity.duration} min</p>
                  </div>
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
                background: "white",
                borderRadius: "16px",
                padding: "24px",
                marginBottom: "24px",
                border: "1px solid #e5e5e5",
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
                    }}
                  >
                    Achievement Progress
                  </h3>
                  <p style={{ fontSize: "14px", color: "#666" }}>
                    You've earned {achievements.length} of 24 badges
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
                    25%
                  </div>
                  <div style={{ fontSize: "14px", color: "#666" }}>
                    Complete
                  </div>
                </div>
              </div>
              <div
                style={{
                  height: "12px",
                  background: "#f0f0f0",
                  borderRadius: "6px",
                  marginTop: "16px",
                  overflow: "hidden",
                }}
              >
                <div
                  style={{
                    height: "100%",
                    width: "25%",
                    background: "linear-gradient(90deg, #fc5200, #ff6b35)",
                    borderRadius: "6px",
                  }}
                />
              </div>
            </div>

            <AchievementsGrid>
              {achievements.map((achievement, index) => (
                <div key={index}>
                  <div
                    style={{
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
                      }}
                    >
                      {achievement.title}
                    </div>
                    <div style={{ fontSize: "12px", color: "#666" }}>
                      {achievement.description}
                    </div>
                  </div>
                </div>
              ))}
            </AchievementsGrid>
          </TabContent>
        )}
      </Container>
    </Layout>
  );
};

export default Profile;
