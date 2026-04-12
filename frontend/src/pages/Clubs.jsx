import React, { useEffect, useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { FiSearch, FiMapPin, FiUsers, FiActivity, FiLock, FiCheck, FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ClubService from "../api/ClubService";
import { SkeletonClubCard } from "../components/Skeleton/Skeleton";
import {
  PageHeader,
  PageIcon,
  PageTitle,
  PageSubtitle,
  SearchFilterRow,
  SearchContainer,
  SearchIcon,
  SearchInput,
  CategorySelect,
  StatsGrid,
  StatCard,
  StatValue,
  StatLabel,
  ClubsGrid,
  ClubCard,
  ClubCover,
  ClubEmoji,
  PrivateBadge,
  ClubContent,
  ClubHeader,
  ClubName,
  CategoryBadge,
  ClubDescription,
  ClubLocation,
  ClubStats,
  ClubStat,
  JoinButton,
  EmptyState,
  EmptyIcon,
  EmptyText,
} from "./ClubsStyles";

const getCategoryInfo = (sportType, t) => {
  const categories = {
    RUN: { labelKey: "running", bgColor: "#fff3ed", textColor: "#fc4c02", borderColor: "#fc4c02", emoji: "🏃" },
    RIDE: { labelKey: "cycling", bgColor: "#e8f5e9", textColor: "#4caf50", borderColor: "#4caf50", emoji: "🚴" },
    SWIM: { labelKey: "swimming", bgColor: "#e3f2fd", textColor: "#2196f3", borderColor: "#2196f3", emoji: "🏊" },
    HIKE: { labelKey: "hiking", bgColor: "#fff8e1", textColor: "#ff9800", borderColor: "#ff9800", emoji: "🥾" },
    OTHER: { labelKey: "triathlon", bgColor: "#fce4ec", textColor: "#e91e63", borderColor: "#e91e63", emoji: "🏅" },
    null: { labelKey: "general", bgColor: null, textColor: null, borderColor: null, emoji: "⚡" },
  };
  const entry = categories[sportType] || categories[null];
  return { ...entry, label: t(`clubs.categories.${entry.labelKey}`) };
};

const Clubs = () => {
  const { t } = useTranslation();
  const [clubs, setClubs] = useState([]);
  const [stats, setStats] = useState({ totalClubs: 0, userClubs: 0, totalMembers: 0 });
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [joiningId, setJoiningId] = useState(null);

  const fetchClubs = useCallback(async () => {
    try {
      setLoading(true);
      let res;
      if (searchQuery.trim()) {
        res = await ClubService.searchClubs(searchQuery);
      } else {
        res = await ClubService.getAllClubs(selectedCategory || null);
      }
      setClubs(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
    } finally {
      setLoading(false);
    }
  }, [searchQuery, selectedCategory]);

  const fetchStats = useCallback(async () => {
    try {
      const res = await ClubService.getClubStats();
      setStats(res.data);
    } catch (error) {
    }
  }, []);

  useEffect(() => {
    fetchClubs();
    fetchStats();
  }, [fetchClubs, fetchStats]);

  const handleJoinClub = async (clubId) => {
    setJoiningId(clubId);
    try {
      await ClubService.joinClub(clubId);
      await fetchClubs();
      await fetchStats();
    } catch (error) {
    } finally {
      setJoiningId(null);
    }
  };

  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
  };

  const handleCategoryChange = (e) => {
    setSelectedCategory(e.target.value);
    setSearchQuery("");
  };

  const totalActivities = clubs.reduce((sum, club) => sum + (club.activitiesCount || 0), 0);

  return (
    <Layout>
      <PageHeader>
        <PageIcon>
          <FiUsers />
        </PageIcon>
        <PageTitle>{t("clubs.title")}</PageTitle>
      </PageHeader>
      <PageSubtitle>{t("clubs.subtitle")}</PageSubtitle>

      <SearchFilterRow>
        <SearchContainer>
          <SearchIcon>
            <FiSearch />
          </SearchIcon>
          <SearchInput
            type="text"
            placeholder={t("clubs.searchPlaceholder")}
            value={searchQuery}
            onChange={handleSearchChange}
          />
        </SearchContainer>
        <CategorySelect value={selectedCategory} onChange={handleCategoryChange}>
          <option value="">{t("clubs.allCategories")}</option>
          <option value="RUN">{t("clubs.categoryOptions.running")}</option>
          <option value="RIDE">{t("clubs.categoryOptions.cycling")}</option>
          <option value="SWIM">{t("clubs.categoryOptions.swimming")}</option>
          <option value="OTHER">{t("clubs.categoryOptions.triathlon")}</option>
          <option value="HIKE">{t("clubs.categoryOptions.hiking")}</option>
        </CategorySelect>
      </SearchFilterRow>

      <StatsGrid>
        <StatCard>
          <StatValue>{stats.totalClubs}</StatValue>
          <StatLabel>{t("clubs.totalClubs")}</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color="#fc4c02">{stats.userClubs}</StatValue>
          <StatLabel>{t("clubs.yourClubs")}</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue>{stats.totalMembers}</StatValue>
          <StatLabel>{t("clubs.totalMembers")}</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color="#fc4c02">{totalActivities}</StatValue>
          <StatLabel>{t("clubs.totalActivities")}</StatLabel>
        </StatCard>
      </StatsGrid>

      <ClubsGrid>
        {loading ? (
          <>
            <SkeletonClubCard />
            <SkeletonClubCard />
            <SkeletonClubCard />
            <SkeletonClubCard />
            <SkeletonClubCard />
            <SkeletonClubCard />
          </>
        ) : clubs.length === 0 ? (
          <EmptyState>
            <EmptyIcon>🔍</EmptyIcon>
            <EmptyText>{t("clubs.empty")}</EmptyText>
          </EmptyState>
        ) : (
          clubs.map((club) => {
            const categoryInfo = getCategoryInfo(club.sportType, t);
            return (
              <ClubCard key={club.id}>
                <ClubCover>
                  <ClubEmoji>{categoryInfo.emoji}</ClubEmoji>
                  {club.private && (
                    <PrivateBadge>
                      <FiLock size={16} />
                    </PrivateBadge>
                  )}
                </ClubCover>
                <ClubContent>
                  <ClubHeader>
                    <ClubName>{club.name}</ClubName>
                    <CategoryBadge
                      bgColor={categoryInfo.bgColor}
                      textColor={categoryInfo.textColor}
                      borderColor={categoryInfo.borderColor}
                    >
                      {categoryInfo.label}
                    </CategoryBadge>
                  </ClubHeader>
                  <ClubDescription>{club.description}</ClubDescription>
                  <ClubLocation>
                    <FiMapPin size={14} />
                    {club.city}, {club.country}
                  </ClubLocation>
                  <ClubStats>
                    <ClubStat>
                      <FiUsers size={14} />
                      {club.membersCount}
                    </ClubStat>
                    <ClubStat>
                      <FiActivity size={14} />
                      {club.activitiesCount || 0}
                    </ClubStat>
                  </ClubStats>
                  {club.member ? (
                    <JoinButton isMember disabled>
                      <FiCheck size={16} />
                      {t("clubs.member")}
                    </JoinButton>
                  ) : (
                    <JoinButton
                      onClick={() => handleJoinClub(club.id)}
                      disabled={joiningId === club.id || club.private}
                    >
                      <FiPlus size={16} />
                      {club.private ? t("clubs.privateClub") : t("clubs.joinClub")}
                    </JoinButton>
                  )}
                </ClubContent>
              </ClubCard>
            );
          })
        )}
      </ClubsGrid>
    </Layout>
  );
};

export default Clubs;
