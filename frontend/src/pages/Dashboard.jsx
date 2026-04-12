import React, { useEffect, useState, useRef } from "react";
import { FiActivity, FiClock, FiZap, FiTarget, FiPlus, FiUser, FiSettings, FiLogOut } from "react-icons/fi";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
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
            <h1>{t("dashboard.welcomeBack", { name: user?.firstName || t("dashboard.defaultAthlete") })} <span>🏃</span></h1>
            <p>{t("dashboard.trainingSummary")}</p>
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
              <SectionTitle>{t("dashboard.recentActivities")}</SectionTitle>
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
              <SectionTitle>{t("dashboard.weeklyGoals")}</SectionTitle>
            </SectionHeader>
            <SkeletonGoalProgress />
            <SkeletonGoalProgress />
          </Section>
          <Section>
            <SectionHeader>
              <SectionTitle>{t("dashboard.upcoming")}</SectionTitle>
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
            <h1>{t("dashboard.welcomeBack", { name: user?.firstName || t("dashboard.defaultAthlete") })} <span>🏃</span></h1>
            <p>{t("dashboard.trainingSummary")}</p>
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
              {t("nav.myProfile")}
            </UserDropdownItem>
            <UserDropdownItem to="/settings" onClick={() => setIsDropdownOpen(false)}>
              <FiSettings />
              {t("nav.settings")}
            </UserDropdownItem>
            <UserDropdownDivider />
            <UserDropdownButton onClick={handleLogout}>
              <FiLogOut />
              {t("nav.signOut")}
            </UserDropdownButton>
          </UserDropdown>
        </UserMenuWrapper>
        </div>
      </DashboardHeader>

      <StatsGrid>
        <StatsCard
          icon={<FiActivity />}
          label={t("dashboard.totalDistance")}
          value={monthlyStats?.totalDistance?.toFixed(1) || "0"}
          unit={t("common.kilometers")}
          trend={monthlyStats?.distanceTrendPercentage}
          bgColor="#fff7ed"
          iconColor="#fc4c02"
        />
        <StatsCard
          icon={<FiClock />}
          label={t("dashboard.activeTime")}
          value={formatDurationValue(monthlyStats?.totalDuration)}
          unit={formatDurationUnit(monthlyStats?.totalDuration)}
          trend={monthlyStats?.durationTrendPercentage}
          bgColor="#eff6ff"
          iconColor="#3b82f6"
        />
        <StatsCard
          icon={<FiZap />}
          label={t("dashboard.caloriesBurned")}
          value={monthlyStats?.totalCalories?.toLocaleString() || "0"}
          unit={t("common.kcal")}
          trend={monthlyStats?.caloriesTrendPercentage}
          bgColor="#fdf2f8"
          iconColor="#ec4899"
        />
        <StatsCard
          icon={<FiTarget />}
          label={t("dashboard.activities")}
          value={monthlyStats?.totalActivities || "0"}
          unit={t("dashboard.thisMonth")}
          trend={monthlyStats?.activitiesTrendPercentage}
          bgColor="#f0fdf4"
          iconColor="#22c55e"
        />
      </StatsGrid>

      <DashboardContainer>
        <MainColumn>
          <Section>
            <SectionHeader>
              <SectionTitle>{t("dashboard.recentActivities")}</SectionTitle>
              <ViewAllLink to="/activities">{t("common.viewAll")}</ViewAllLink>
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
                <EmptyText>{t("dashboard.noActivitiesYet")}</EmptyText>
                <AddButton to="/activities/new">
                  <FiPlus size={18} />
                  {t("nav.addActivity")}
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
                <SectionTitle>{t("dashboard.weeklyGoals")}</SectionTitle>
                <ViewAllLink to="/goals">{t("common.manage")}</ViewAllLink>
              </SectionHeader>
              {goals.slice(0, 2).map((goal) => (
                <GoalProgress key={goal.id} goal={goal} />
              ))}
            </Section>
          )}

          {events.length > 0 && (
            <Section>
              <EventsHeader>
                <EventsTitle>{t("dashboard.upcoming")}</EventsTitle>
                <EventsViewAllLink to="/events">{t("common.viewAll")}</EventsViewAllLink>
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
