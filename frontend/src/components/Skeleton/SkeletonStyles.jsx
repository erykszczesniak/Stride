import styled, { keyframes } from "styled-components";

export const shimmer = keyframes`
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
`;

export const SkeletonBase = styled.div`
  background: ${({ theme }) => theme.bgHover};
  background-image: linear-gradient(
    90deg,
    ${({ theme }) => theme.bgHover} 0px,
    ${({ theme }) => theme.bgSecondary} 40px,
    ${({ theme }) => theme.bgHover} 80px
  );
  background-size: 200px 100%;
  animation: ${shimmer} 1.5s ease-in-out infinite;
  border-radius: ${({ $radius }) => $radius || "8px"};
`;

export const SkeletonText = styled(SkeletonBase)`
  height: ${({ $height }) => $height || "16px"};
  width: ${({ $width }) => $width || "100%"};
  margin-bottom: ${({ $mb }) => $mb || "0"};
`;

export const SkeletonCircle = styled(SkeletonBase)`
  width: ${({ $size }) => $size || "44px"};
  height: ${({ $size }) => $size || "44px"};
  border-radius: 50%;
  flex-shrink: 0;
`;

export const SkeletonRect = styled(SkeletonBase)`
  width: ${({ $width }) => $width || "100%"};
  height: ${({ $height }) => $height || "100px"};
`;

export const ActivityCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

export const CardHeaderInfo = styled.div`
  flex: 1;
`;

export const StatsRow = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  margin-bottom: 16px;
`;

export const StatItem = styled.div`
  flex: 1;
`;

export const CardFooter = styled.div`
  display: flex;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid ${({ theme }) => theme.border};
`;

export const StatsCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const StatsCardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
`;

export const EventCardWrapper = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const EventDateBox = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
`;

export const EventInfo = styled.div`
  flex: 1;
`;

export const ChartWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 24px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const ChartHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
`;

export const ChartBars = styled.div`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 150px;
  gap: 8px;
`;

export const GoalWrapper = styled.div`
  padding: 16px 0;
  border-bottom: 1px solid ${({ theme }) => theme.border};

  &:last-child {
    border-bottom: none;
  }
`;

export const GoalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
`;

export const ClubCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const ClubCover = styled.div`
  height: 140px;
  background: ${({ theme }) => theme.bgHover};
`;

export const ClubContent = styled.div`
  padding: 16px 20px 20px;
`;

export const ClubHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
`;
