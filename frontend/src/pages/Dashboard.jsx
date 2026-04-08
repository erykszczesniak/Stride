import React, { useEffect, useState, useRef } from "react";
import { FiActivity, FiClock, FiZap, FiTarget, FiPlus, FiUser, FiSettings, FiLogOut } from "react-icons/fi";
import { useNavigate } from "react-router-dom";
import Layout from "../components/Layout/Layout";
import NotificationDropdown from "../components/Navbar/NotificationDropdown";
import StatsCard from "../components/StatsCard/StatsCard";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import WeeklyChart from "../components/WeeklyChart/WeeklyChart";
import GoalProgress from "../components/GoalProgress/GoalProgress";
import EventCard from "../components/EventCard/EventCard";
import { EventsList, EventsHeader, EventsTitle, ViewAllLink as EventsViewAllLink } from "../components/EventCard/EventCardStyles";
import {
  SkeletonActivityCard,
  SkeletonStatsCard,
  SkeletonChart,
  SkeletonGoalProgress,
  SkeletonEventCard,
} from "../components/Skeleton/Skeleton";
import ActivityService from "../api/ActivityService";
import GoalService from "../api/GoalService";
import EventService from "../api/EventService";
import { useAuth } from "../context/AuthContext";
import { formatDurationValue, formatDurationUnit } from "../utils/formatters";
import {
  DashboardContainer,
  DashboardHeader,
  MainColumn,
  SideColumn,
  Section,
  SectionHeader,
  SectionTitle,
  ActivitiesList,
  EmptyState,
  EmptyIcon,
  EmptyText,
  AddButton,
  StatsGrid,
  WelcomeSection,
  WelcomeText,
  UserAvatar,
  UserMenuWrapper,
  UserDropdown,
  UserDropdownHeader,
  UserDropdownName,
  UserDropdownEmail,
  UserDropdownItem,
  UserDropdownButton,
  UserDropdownDivider,
  ViewAllLink,
} from "./DashboardStyles";

const Dashboard = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [monthlyStats, setMonthlyStats] = useState(null);
  const [weeklyStats, setWeeklyStats] = useState(null);
  const [activities, setActivities] = useState([]);
  const [goals, setGoals] = useState([]);
  const [events, setEvents] = useState([]);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        const [monthlyRes, weeklyRes, activitiesRes, goalsRes, eventsRes] = await Promise.all([
          ActivityService.getMonthlyStats().catch(() => ({ data: null })),
          ActivityService.getWeeklyStats().catch(() => ({ data: null })),
          ActivityService.getFeedActivities(0, 5).catch(() => ({ data: [] })),
          GoalService.getUserGoals().catch(() => ({ data: [] })),
          EventService.getUpcomingEvents(0, 3).catch(() => ({ data: [] })),
        ]);

        setMonthlyStats(monthlyRes.data);
        setWeeklyStats(weeklyRes.data);
        setActivities(Array.isArray(activitiesRes.data) ? activitiesRes.data : []);
        setGoals(Array.isArray(goalsRes.data) ? goalsRes.data : []);
        setEvents(Array.isArray(eventsRes.data) ? eventsRes.data : []);
      } catch (error) {
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const handleLikeActivity = async (activityId, isLiked) => {
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
    }
  };

  const renderSkeletonLoading = () => (
    <>
      <DashboardHeader>
        <WelcomeSection>
          <WelcomeText>
            <h1>Welcome back, {user?.firstName || 'Athlete'}! <span>🏃</span></h1>
            <p>Here's your training summary</p>
          </WelcomeText>
        </WelcomeSection>
        <UserAvatar>{user?.firstName?.[0] || 'A'}</UserAvatar>
      </DashboardHeader>

      <StatsGrid>
        <SkeletonStatsCard />
        <SkeletonStatsCard />
        <SkeletonStatsCard />
        <SkeletonStatsCard />
      </StatsGrid>

      <DashboardContainer>
        <MainColumn>
          <Section>
            <SectionHeader>
              <SectionTitle>Recent Activities</SectionTitle>
            </SectionHeader>
            <ActivitiesList>
              <SkeletonActivityCard />
              <SkeletonActivityCard />
              <SkeletonActivityCard />
            </ActivitiesList>
          </Section>
        </MainColumn>

        <SideColumn>
          <SkeletonChart />
          <Section>
            <SectionHeader>
              <SectionTitle>Weekly Goals</SectionTitle>
            </SectionHeader>
            <SkeletonGoalProgress />
            <SkeletonGoalProgress />
          </Section>
          <Section>
            <SectionHeader>
              <SectionTitle>Upcoming</SectionTitle>
            </SectionHeader>
            <EventsList>
              <SkeletonEventCard />
              <SkeletonEventCard />
            </EventsList>
          </Section>
        </SideColumn>
      </DashboardContainer>
    </>
  );

  return (
    <Layout>
      {loading ? renderSkeletonLoading() : (
        <>
      <DashboardHeader>
        <WelcomeSection>
          <WelcomeText>
            <h1>Welcome back, {user?.firstName || 'Athlete'}! <span>🏃</span></h1>
            <p>Here's your training summary</p>
          </WelcomeText>
        </WelcomeSection>
        <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
          <NotificationDropdown />
        <UserMenuWrapper ref={dropdownRef}>
          <UserAvatar onClick={() => setIsDropdownOpen(!isDropdownOpen)}>
            {user?.firstName?.[0] || 'A'}
          </UserAvatar>
          <UserDropdown $isOpen={isDropdownOpen}>
            <UserDropdownHeader>
              <UserDropdownName>{user?.firstName} {user?.lastName}</UserDropdownName>
              <UserDropdownEmail>{user?.email}</UserDropdownEmail>
            </UserDropdownHeader>
            <UserDropdownItem to="/profile" onClick={() => setIsDropdownOpen(false)}>
              <FiUser />
              My Profile
            </UserDropdownItem>
            <UserDropdownItem to="/settings" onClick={() => setIsDropdownOpen(false)}>
              <FiSettings />
              Settings
            </UserDropdownItem>
            <UserDropdownDivider />
            <UserDropdownButton onClick={handleLogout}>
              <FiLogOut />
              Sign Out
            </UserDropdownButton>
          </UserDropdown>
        </UserMenuWrapper>
        </div>
      </DashboardHeader>

      <StatsGrid>
        <StatsCard
          icon={<FiActivity />}
          label="Total Distance"
          value={monthlyStats?.totalDistance?.toFixed(1) || "0"}
          unit="km"
          trend={monthlyStats?.distanceTrendPercentage}
          bgColor="#fff7ed"
          iconColor="#fc4c02"
        />
        <StatsCard
          icon={<FiClock />}
          label="Active Time"
          value={formatDurationValue(monthlyStats?.totalDuration)}
          unit={formatDurationUnit(monthlyStats?.totalDuration)}
          trend={monthlyStats?.durationTrendPercentage}
          bgColor="#eff6ff"
          iconColor="#3b82f6"
        />
        <StatsCard
          icon={<FiZap />}
          label="Calories Burned"
          value={monthlyStats?.totalCalories?.toLocaleString() || "0"}
          unit="kcal"
          trend={monthlyStats?.caloriesTrendPercentage}
          bgColor="#fdf2f8"
          iconColor="#ec4899"
        />
        <StatsCard
          icon={<FiTarget />}
          label="Activities"
          value={monthlyStats?.totalActivities || "0"}
          unit="this month"
          trend={monthlyStats?.activitiesTrendPercentage}
          bgColor="#f0fdf4"
          iconColor="#22c55e"
        />
      </StatsGrid>

      <DashboardContainer>
        <MainColumn>
          <Section>
            <SectionHeader>
              <SectionTitle>Recent Activities</SectionTitle>
              <ViewAllLink to="/activities">View All</ViewAllLink>
            </SectionHeader>

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
                <EmptyText>No activities yet. Start tracking your workouts!</EmptyText>
                <AddButton to="/activities/new">
                  <FiPlus size={18} />
                  Add Activity
                </AddButton>
              </EmptyState>
            )}
          </Section>
        </MainColumn>

        <SideColumn>
          <WeeklyChart
            data={weeklyStats?.dailyStats}
            trendPercentage={weeklyStats?.trendPercentage}
          />

          {goals.length > 0 && (
            <Section>
              <SectionHeader>
                <SectionTitle>Weekly Goals</SectionTitle>
                <ViewAllLink to="/goals">Manage</ViewAllLink>
              </SectionHeader>
              {goals.slice(0, 2).map((goal) => (
                <GoalProgress key={goal.id} goal={goal} />
              ))}
            </Section>
          )}

          {events.length > 0 && (
            <Section>
              <EventsHeader>
                <EventsTitle>Upcoming</EventsTitle>
                <EventsViewAllLink to="/events">View All</EventsViewAllLink>
              </EventsHeader>
              <EventsList>
                {events.map((event) => (
                  <EventCard key={event.id} event={event} compact />
                ))}
              </EventsList>
            </Section>
          )}
        </SideColumn>
      </DashboardContainer>
        </>
      )}
    </Layout>
  );
};

export default Dashboard;
