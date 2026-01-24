import React, { useEffect, useState } from "react";
import { FiActivity, FiClock, FiZap, FiTarget, FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import StatsCard from "../components/StatsCard/StatsCard";
import ActivityCard from "../components/ActivityCard/ActivityCard";
import WeeklyChart from "../components/WeeklyChart/WeeklyChart";
import GoalProgress from "../components/GoalProgress/GoalProgress";
import EventCard from "../components/EventCard/EventCard";
import { EventsList, EventsHeader, EventsTitle, ViewAllLink } from "../components/EventCard/EventCardStyles";
import ActivityService from "../api/ActivityService";
import GoalService from "../api/GoalService";
import EventService from "../api/EventService";
import {
  DashboardContainer,
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
  WelcomeAvatar,
  WelcomeText,
} from "./DashboardStyles";

const Dashboard = () => {
  const [loading, setLoading] = useState(true);
  const [monthlyStats, setMonthlyStats] = useState(null);
  const [weeklyStats, setWeeklyStats] = useState(null);
  const [activities, setActivities] = useState([]);
  const [goals, setGoals] = useState([]);
  const [events, setEvents] = useState([]);

  const userId = 1; // Hardcoded for now

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
      // Refresh activities
      const res = await ActivityService.getFeedActivities(userId, 0, 5);
      setActivities(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error liking activity:", error);
    }
  };

  const formatDuration = (seconds) => {
    if (!seconds) return "0h";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
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
      <WelcomeSection>
        <WelcomeAvatar>JD</WelcomeAvatar>
        <WelcomeText>
          <h1>Welcome back, John!</h1>
          <p>Here's your training summary</p>
        </WelcomeText>
      </WelcomeSection>

      <StatsGrid>
        <StatsCard
          icon={<FiActivity />}
          label="Total Distance"
          value={`${monthlyStats?.totalDistance?.toFixed(1) || 0} km`}
          trend={monthlyStats?.trendPercentage}
          bgColor="#fff3e0"
          iconColor="#fc4c02"
        />
        <StatsCard
          icon={<FiClock />}
          label="Active Time"
          value={formatDuration(monthlyStats?.totalDuration)}
          bgColor="#e3f2fd"
          iconColor="#1976d2"
        />
        <StatsCard
          icon={<FiZap />}
          label="Calories Burned"
          value={`${monthlyStats?.totalCalories?.toLocaleString() || 0} kcal`}
          bgColor="#fce4ec"
          iconColor="#c2185b"
        />
        <StatsCard
          icon={<FiTarget />}
          label="Activities"
          value={monthlyStats?.totalActivities || 0}
          bgColor="#e8f5e9"
          iconColor="#388e3c"
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
                <EventsTitle>Upcoming Events</EventsTitle>
                <ViewAllLink to="/events">View All</ViewAllLink>
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
