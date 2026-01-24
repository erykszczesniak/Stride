import styled from "styled-components";

export const LayoutContainer = styled.div`
  min-height: 100vh;
  background-color: #f5f5f5;
`;

export const MainContent = styled.main`
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #333;
  margin-bottom: 24px;
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
  background-color: #fc4c02;
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
    color: #333;
    margin-bottom: 4px;
  }

  p {
    font-size: 14px;
    color: #666;
  }
`;
