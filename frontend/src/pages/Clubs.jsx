import React, { useEffect, useState, useCallback } from "react";
import { FiSearch, FiMapPin, FiUsers, FiActivity, FiLock, FiCheck, FiPlus } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ClubService from "../api/ClubService";
import styled from "styled-components";

const PageHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
`;

const PageIcon = styled.div`
  color: #fc4c02;
  font-size: 28px;
  display: flex;
  align-items: center;
`;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin: 0;
`;

const PageSubtitle = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 24px 0;
`;

const SearchFilterRow = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
`;

const SearchContainer = styled.div`
  flex: 1;
  position: relative;
`;

const SearchIcon = styled.div`
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const CategorySelect = styled.select`
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  min-width: 160px;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
`;

const StatCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const StatValue = styled.div`
  font-size: 28px;
  font-weight: 700;
  color: ${(props) => props.color || "#333"};
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: #666;
`;

const ClubsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const ClubCard = styled.div`
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const ClubCover = styled.div`
  height: 140px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff8c00 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
`;

const ClubEmoji = styled.span`
  font-size: 64px;
`;

const PrivateBadge = styled.div`
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
`;

const ClubContent = styled.div`
  padding: 16px 20px 20px;
`;

const ClubHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
`;

const ClubName = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
`;

const CategoryBadge = styled.span`
  background-color: ${(props) => props.bgColor || "#f5f5f5"};
  color: ${(props) => props.textColor || "#666"};
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid ${(props) => props.borderColor || "#e0e0e0"};
`;

const ClubDescription = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

const ClubLocation = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #888;
  margin-bottom: 12px;
`;

const ClubStats = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
`;

const ClubStat = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
`;

const JoinButton = styled.button`
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;

  ${(props) =>
    props.isMember
      ? `
    background-color: white;
    border: 1px solid #e0e0e0;
    color: #333;
  `
      : `
    background-color: #fc4c02;
    color: white;
    &:hover {
      background-color: #e04400;
    }
  `}

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
`;

const EmptyState = styled.div`
  grid-column: 1 / -1;
  background: white;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

const EmptyText = styled.p`
  font-size: 16px;
  color: #666;
  margin: 0;
`;

const LoadingSpinner = styled.div`
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  padding: 60px;
  color: #666;
`;

const getCategoryInfo = (sportType) => {
  const categories = {
    RUN: { label: "running", bgColor: "#fff3ed", textColor: "#fc4c02", borderColor: "#fc4c02", emoji: "🏃" },
    RIDE: { label: "cycling", bgColor: "#e8f5e9", textColor: "#4caf50", borderColor: "#4caf50", emoji: "🚴" },
    SWIM: { label: "swimming", bgColor: "#e3f2fd", textColor: "#2196f3", borderColor: "#2196f3", emoji: "🏊" },
    HIKE: { label: "hiking", bgColor: "#fff8e1", textColor: "#ff9800", borderColor: "#ff9800", emoji: "🥾" },
    OTHER: { label: "triathlon", bgColor: "#fce4ec", textColor: "#e91e63", borderColor: "#e91e63", emoji: "🏅" },
    null: { label: "general", bgColor: "#f5f5f5", textColor: "#666", borderColor: "#e0e0e0", emoji: "⚡" },
  };
  return categories[sportType] || categories[null];
};

const Clubs = () => {
  const [clubs, setClubs] = useState([]);
  const [stats, setStats] = useState({ totalClubs: 0, userClubs: 0, totalMembers: 0 });
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [joiningId, setJoiningId] = useState(null);
  const userId = 1;

  const fetchClubs = useCallback(async () => {
    try {
      setLoading(true);
      let res;
      if (searchQuery.trim()) {
        res = await ClubService.searchClubs(searchQuery, userId);
      } else {
        res = await ClubService.getAllClubs(userId, selectedCategory || null);
      }
      setClubs(Array.isArray(res.data) ? res.data : []);
    } catch (error) {
      console.error("Error fetching clubs:", error);
    } finally {
      setLoading(false);
    }
  }, [searchQuery, selectedCategory, userId]);

  const fetchStats = useCallback(async () => {
    try {
      const res = await ClubService.getClubStats(userId);
      setStats(res.data);
    } catch (error) {
      console.error("Error fetching stats:", error);
    }
  }, [userId]);

  useEffect(() => {
    fetchClubs();
    fetchStats();
  }, [fetchClubs, fetchStats]);

  const handleJoinClub = async (clubId) => {
    setJoiningId(clubId);
    try {
      await ClubService.joinClub(clubId, userId);
      await fetchClubs();
      await fetchStats();
    } catch (error) {
      console.error("Error joining club:", error);
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
        <PageTitle>Clubs</PageTitle>
      </PageHeader>
      <PageSubtitle>Join communities and train together</PageSubtitle>

      <SearchFilterRow>
        <SearchContainer>
          <SearchIcon>
            <FiSearch />
          </SearchIcon>
          <SearchInput
            type="text"
            placeholder="Search clubs..."
            value={searchQuery}
            onChange={handleSearchChange}
          />
        </SearchContainer>
        <CategorySelect value={selectedCategory} onChange={handleCategoryChange}>
          <option value="">All Categories</option>
          <option value="RUN">Running</option>
          <option value="RIDE">Cycling</option>
          <option value="SWIM">Swimming</option>
          <option value="OTHER">Triathlon</option>
          <option value="HIKE">Hiking</option>
        </CategorySelect>
      </SearchFilterRow>

      <StatsGrid>
        <StatCard>
          <StatValue>{stats.totalClubs}</StatValue>
          <StatLabel>Total Clubs</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color="#fc4c02">{stats.userClubs}</StatValue>
          <StatLabel>Your Clubs</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue>{stats.totalMembers}</StatValue>
          <StatLabel>Total Members</StatLabel>
        </StatCard>
        <StatCard>
          <StatValue color="#fc4c02">{totalActivities}</StatValue>
          <StatLabel>Total Activities</StatLabel>
        </StatCard>
      </StatsGrid>

      <ClubsGrid>
        {loading ? (
          <LoadingSpinner>Loading clubs...</LoadingSpinner>
        ) : clubs.length === 0 ? (
          <EmptyState>
            <EmptyIcon>🔍</EmptyIcon>
            <EmptyText>No clubs found. Try a different search or category.</EmptyText>
          </EmptyState>
        ) : (
          clubs.map((club) => {
            const categoryInfo = getCategoryInfo(club.sportType);
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
                      Member
                    </JoinButton>
                  ) : (
                    <JoinButton
                      onClick={() => handleJoinClub(club.id)}
                      disabled={joiningId === club.id || club.private}
                    >
                      <FiPlus size={16} />
                      {club.private ? "Private Club" : "Join Club"}
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
