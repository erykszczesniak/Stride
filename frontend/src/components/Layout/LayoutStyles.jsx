import styled from "styled-components";

export const LayoutContainer = styled.div`
  min-height: 100vh;
  background: ${({ theme }) => theme.bgPrimary};
  display: flex;
  transition: background-color 0.3s ease;
`;

export const MainContent = styled.main`
  flex: 1;
  margin-left: 260px;
  min-height: 100vh;

  @media (max-width: 1024px) {
    margin-left: 80px;
  }
`;

export const TopBar = styled.div`
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 8px 40px;
  background: ${({ theme }) => theme.bgPrimary};
  transition: background-color 0.3s ease;

  @media (max-width: 768px) {
    padding: 8px 20px;
  }
`;

export const ContentWrapper = styled.div`
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 40px;

  @media (max-width: 768px) {
    padding: 24px 20px;
  }
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 24px;
  transition: color 0.3s ease;
`;

export const WelcomeSection = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
`;

export const WelcomeAvatar = styled.div`
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  font-size: 20px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const WelcomeText = styled.div`
  h1 {
    font-size: 24px;
    font-weight: 700;
    color: ${({ theme }) => theme.textPrimary};
    margin-bottom: 4px;
    transition: color 0.3s ease;
  }

  p {
    font-size: 14px;
    color: ${({ theme }) => theme.textSecondary};
    transition: color 0.3s ease;
  }
`;
