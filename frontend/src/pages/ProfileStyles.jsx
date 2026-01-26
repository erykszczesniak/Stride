import styled from 'styled-components';

export const Container = styled.div`
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px 60px;
`;

export const ProfileHeader = styled.div`
  background: white;
  border-bottom: 1px solid #e5e5e5;
  margin-bottom: 32px;
`;

export const CoverImage = styled.div`
  height: 200px;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  position: relative;
`;

export const ProfileContent = styled.div`
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding-bottom: 24px;

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
`;

export const Avatar = styled.div`
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  border: 6px solid white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  font-weight: 700;
  color: white;
  margin-top: -80px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }

  span {
    font-size: 64px;
    font-weight: 700;
    color: white;
  }

  @media (max-width: 768px) {
    width: 120px;
    height: 120px;
    font-size: 48px;

    span {
      font-size: 48px;
    }
  }
`;

export const ProfileInfo = styled.div`
  flex: 1;
  padding-bottom: 8px;
`;

export const ProfileName = styled.h1`
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;

  @media (max-width: 768px) {
    font-size: 28px;
  }
`;

export const ProfileBio = styled.p`
  font-size: 16px;
  color: #666;
  margin: 0 0 4px 0;
`;

export const ProfileLocation = styled.p`
  font-size: 14px;
  color: #999;
  margin: 0 0 4px 0;
`;

export const EditButton = styled.button`
  padding: 12px 24px;
  background: white;
  border: 2px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;

  &:hover {
    border-color: #fc5200;
    color: #fc5200;
  }
`;

export const ProfileStats = styled.div`
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
  padding-bottom: 24px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
`;

export const StatItem = styled.div`
  text-align: center;
`;

export const StatValue = styled.div`
  font-size: 32px;
  font-weight: 900;
  color: #1a1a1a;
  margin-bottom: 4px;

  @media (max-width: 768px) {
    font-size: 24px;
  }
`;

export const StatLabel = styled.div`
  font-size: 14px;
  color: #666;
  font-weight: 500;
`;

export const TabsContainer = styled.div`
  display: flex;
  gap: 8px;
  background: white;
  border: 1px solid #e5e5e5;
  padding: 4px;
  border-radius: 12px;
  margin-bottom: 32px;
  width: fit-content;

  @media (max-width: 768px) {
    width: 100%;
  }
`;

export const Tab = styled.button`
  padding: 12px 24px;
  background: ${props => props.active ? '#1a1a1a' : 'transparent'};
  color: ${props => props.active ? 'white' : '#666'};
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  flex: 1;

  &:hover {
    background: ${props => props.active ? '#1a1a1a' : '#f5f5f5'};
  }
`;

export const TabContent = styled.div`
  animation: fadeIn 0.3s ease;

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
`;

export const SectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 24px 0;
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 48px;
`;

export const StatCard = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1px solid #e5e5e5;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
`;

export const StatIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
`;

export const MonthlyChart = styled.div`
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  background: white;
  border-radius: 16px;
  padding: 32px;
  border: 1px solid #e5e5e5;
  margin-bottom: 48px;

  @media (max-width: 768px) {
    gap: 8px;
    padding: 24px;
  }
`;

export const MonthBar = styled.div`
  height: 150px;
  display: flex;
  align-items: flex-end;
  justify-content: center;

  > div {
    width: 100%;
    background: linear-gradient(180deg, #fc5200 0%, #ff7746 100%);
    border-radius: 8px 8px 0 0;
    height: ${props => props.height}%;
    min-height: 20px;
    transition: all 0.3s ease;

    &:hover {
      background: linear-gradient(180deg, #ff7746 0%, #fc5200 100%);
    }
  }
`;

export const MonthLabel = styled.div`
  text-align: center;
  font-size: 12px;
  color: #666;
  margin-top: 8px;
  font-weight: 600;
`;

export const MonthValue = styled.div`
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: #1a1a1a;
  margin-top: 4px;
`;

export const AchievementsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
`;

export const LoadingSpinner = styled.div`
  text-align: center;
  padding: 60px 20px;
  font-size: 16px;
  color: #666;
`;

export const EmptyState = styled.div`
  background: white;
  border-radius: 16px;
  padding: 60px 20px;
  text-align: center;
  font-size: 16px;
  color: #666;
  border: 1px solid #e5e5e5;
`;
