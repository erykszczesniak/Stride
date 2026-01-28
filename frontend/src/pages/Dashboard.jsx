import React, { useEffect, useState } from "react";
import { FiActivity, FiClock, FiZap, FiTarget, FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import StatsCard from "../components/StatsCard/StatsCard";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import WeeklyChart from "../components/WeeklyChart/WeeklyChart";
import GoalProgress from "../components/GoalProgress/GoalProgress";
import EventCard from "../components/EventCard/EventCard";
import { EventsList, EventsHeader, EventsTitle, ViewAllLink as EventsViewAllLink } from "../components/EventCard/EventCardStyles";
import ActivityService from "../api/ActivityService";
import GoalService from "../api/GoalService";
import EventService from "../api/EventService";
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
  LoadingSpinner,
  StatsGrid,
  WelcomeSection,
  WelcomeText,
  UserAvatar,
  ViewAllLink,
} from "./DashboardStyles";

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [monthlyStats, setMonthlyStats] = useState(null);
  const [weeklyStats, setWeeklyStats] = useState(null);
  const [activities, setActivities] = useState([]);
  const [goals, setGoals] = useState([]);
  const [events, setEvents] = useState([]);

  const userId = 1;

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);

        const [monthlyRes, weeklyRes, activitiesRes, goalsRes, eventsRes] = await Promise.all([
          ActivityService.getMonthlyStats(userId).catch(() => ({ data: null })),
          ActivityService.getWeeklyStats(userId).catch(() => ({ data: null })),
          ActivityService.getFeedActivities(userId, 0, 5).catch(() => ({ data: [] })),
          GoalService.getActiveGoals(userId).catch(() => ({ data: [] })),
          EventService.getUpcomingEvents(0, 3).catch(() => ({ data: [] })),
        ]);

        setMonthlyStats(monthlyRes.data);
        setWeeklyStats(weeklyRes.data);
        setActivities(Array.isArray(activitiesRes.data) ? activitiesRes.data : []);
        setGoals(Array.isArray(goalsRes.data) ? goalsRes.data : []);
        setEvents(Array.isArray(eventsRes.data) ? eventsRes.data : []);
      } catch (error) {
        console.error("Error fetching dashboard data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [userId]);

  const handleLikeActivity = async (activityId) => {
    try {
      await ActivityService.likeActivity(activityId, userId);
      const res = await ActivityService.getFeedActivities(userId, 0, 5);
      setActivities(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error liking activity:", error);
    }
  };

  const formatDuration = (seconds) => {
    if (!seconds) return "0";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) return `${hours}`;
    return `${minutes}m`;
  };

  const formatDurationUnit = (seconds) => {
    if (!seconds) return "hours";
    const hours = Math.floor(seconds / 3600);
    if (hours > 0) return "hours";
    return "min";
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <DashboardHeader>
        <WelcomeSection>
          <WelcomeText>
            <h1>Welcome back, Pan! <span>🏃</span></h1>
            <p>Here's your training summary</p>
          </WelcomeText>
        </WelcomeSection>
        <UserAvatar>P</UserAvatar>
      </DashboardHeader>

      <StatsGrid>
        <StatsCard
          icon={<FiActivity />}
          label="Total Distance"
          value={monthlyStats?.totalDistance?.toFixed(1) || "0"}
          unit="km"
          trend={monthlyStats?.trendPercentage || 8.2}
          bgColor="#fff7ed"
          iconColor="#fc4c02"
        />
        <StatsCard
          icon={<FiClock />}
          label="Active Time"
          value={formatDuration(monthlyStats?.totalDuration)}
          unit={formatDurationUnit(monthlyStats?.totalDuration)}
          trend={12}
          bgColor="#eff6ff"
          iconColor="#3b82f6"
        />
        <StatsCard
          icon={<FiZap />}
          label="Calories Burned"
          value={monthlyStats?.totalCalories?.toLocaleString() || "0"}
          unit="kcal"
          trend={5.3}
          bgColor="#fdf2f8"
          iconColor="#ec4899"
        />
        <StatsCard
          icon={<FiTarget />}
          label="Activities"
          value={monthlyStats?.totalActivities || "0"}
          unit="this month"
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
                  <EventCard key={event.id} event={event} />
                ))}
              </EventsList>
            </Section>
          )}
        </SideColumn>
      </DashboardContainer>
    </Layout>
  );
};

export default Dashboard;
