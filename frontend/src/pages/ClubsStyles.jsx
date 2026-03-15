import styled from "styled-components";

export const PageHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
`;

export const PageIcon = styled.div`
  color: #fc4c02;
  font-size: 28px;
  display: flex;
  align-items: center;
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const PageSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 24px 0;
  transition: color 0.3s ease;
`;

export const SearchFilterRow = styled.div`
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
`;

export const SearchContainer = styled.div`
  flex: 1;
  position: relative;
`;

export const SearchIcon = styled.div`
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: ${({ theme }) => theme.textMuted};
`;

export const SearchInput = styled.input`
  width: 100%;
  padding: 12px 16px 12px 44px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.3s ease;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
  }

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
  }
`;

export const CategorySelect = styled.select`
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  min-width: 160px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
  }
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
`;

export const StatCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const StatValue = styled.div`
  font-size: 28px;
  font-weight: 700;
  color: ${(props) => props.color || props.theme.textPrimary};
  margin-bottom: 4px;
  transition: color 0.3s ease;
`;

export const StatLabel = styled.div`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

export const ClubsGrid = styled.div`
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

export const ClubCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const ClubCover = styled.div`
  height: 140px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff8c00 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
`;

export const ClubEmoji = styled.span`
  font-size: 64px;
`;

export const PrivateBadge = styled.div`
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

export const ClubContent = styled.div`
  padding: 16px 20px 20px;
`;

export const ClubHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
`;

export const ClubName = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const CategoryBadge = styled.span`
  background-color: ${(props) => props.bgColor || props.theme.bgSecondary};
  color: ${(props) => props.textColor || props.theme.textSecondary};
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid ${(props) => props.borderColor || props.theme.border};
`;

export const ClubDescription = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s ease;
`;

export const ClubLocation = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: ${({ theme }) => theme.textMuted};
  margin-bottom: 12px;
  transition: color 0.3s ease;
`;

export const ClubStats = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
`;

export const ClubStat = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

export const JoinButton = styled.button`
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
    background-color: ${props.theme.bgCard};
    border: 1px solid ${props.theme.border};
    color: ${props.theme.textPrimary};
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

export const EmptyState = styled.div`
  grid-column: 1 / -1;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

export const EmptyText = styled.p`
  font-size: 16px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
  transition: color 0.3s ease;
`;
