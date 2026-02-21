import React from "react";
import styled, { keyframes } from "styled-components";

const shimmer = keyframes`
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
`;

const SkeletonBase = styled.div`
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

// Activity Card Skeleton
const ActivityCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.border};
`;

const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

const CardHeaderInfo = styled.div`
  flex: 1;
`;

const StatsRow = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  margin-bottom: 16px;
`;

const StatItem = styled.div`
  flex: 1;
`;

const CardFooter = styled.div`
  display: flex;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid ${({ theme }) => theme.border};
`;

export const SkeletonActivityCard = () => (
  <ActivityCardWrapper>
    <CardHeader>
      <SkeletonCircle $size="44px" />
      <CardHeaderInfo>
        <SkeletonText $width="120px" $height="14px" $mb="6px" />
        <SkeletonText $width="80px" $height="12px" />
      </CardHeaderInfo>
      <SkeletonRect $width="40px" $height="40px" $radius="12px" />
    </CardHeader>
    <SkeletonText $width="70%" $height="18px" $mb="8px" />
    <SkeletonText $width="90%" $height="14px" $mb="16px" />
    <StatsRow>
      <StatItem>
        <SkeletonText $width="50px" $height="12px" $mb="4px" />
        <SkeletonText $width="70px" $height="16px" />
      </StatItem>
      <StatItem>
        <SkeletonText $width="50px" $height="12px" $mb="4px" />
        <SkeletonText $width="70px" $height="16px" />
      </StatItem>
      <StatItem>
        <SkeletonText $width="50px" $height="12px" $mb="4px" />
        <SkeletonText $width="70px" $height="16px" />
      </StatItem>
    </StatsRow>
    <CardFooter>
      <SkeletonText $width="80px" $height="32px" $radius="8px" />
      <SkeletonText $width="80px" $height="32px" $radius="8px" />
    </CardFooter>
  </ActivityCardWrapper>
);

// Stats Card Skeleton
const StatsCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.border};
`;

const StatsCardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
`;

export const SkeletonStatsCard = () => (
  <StatsCardWrapper>
    <StatsCardHeader>
      <SkeletonRect $width="44px" $height="44px" $radius="12px" />
      <SkeletonText $width="60px" $height="24px" $radius="20px" />
    </StatsCardHeader>
    <SkeletonText $width="80px" $height="12px" $mb="6px" />
    <SkeletonText $width="100px" $height="28px" />
  </StatsCardWrapper>
);

// Event Card Skeleton
const EventCardWrapper = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.border};
`;

const EventDateBox = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
`;

const EventInfo = styled.div`
  flex: 1;
`;

export const SkeletonEventCard = () => (
  <EventCardWrapper>
    <EventDateBox>
      <SkeletonText $width="30px" $height="12px" />
      <SkeletonText $width="24px" $height="24px" />
    </EventDateBox>
    <EventInfo>
      <SkeletonText $width="70%" $height="16px" $mb="8px" />
      <SkeletonText $width="50%" $height="12px" />
    </EventInfo>
  </EventCardWrapper>
);

// Chart Skeleton
const ChartWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 24px;
  border: 1px solid ${({ theme }) => theme.border};
`;

const ChartHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
`;

const ChartBars = styled.div`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 150px;
  gap: 8px;
`;

export const SkeletonChart = () => (
  <ChartWrapper>
    <ChartHeader>
      <SkeletonText $width="120px" $height="18px" />
      <SkeletonText $width="80px" $height="24px" $radius="12px" />
    </ChartHeader>
    <ChartBars>
      {[60, 80, 45, 90, 70, 55, 85].map((height, i) => (
        <SkeletonRect key={i} $width="100%" $height={`${height}%`} $radius="8px 8px 0 0" />
      ))}
    </ChartBars>
  </ChartWrapper>
);

// Goal Progress Skeleton
const GoalWrapper = styled.div`
  padding: 16px 0;
  border-bottom: 1px solid ${({ theme }) => theme.border};

  &:last-child {
    border-bottom: none;
  }
`;

const GoalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
`;

export const SkeletonGoalProgress = () => (
  <GoalWrapper>
    <GoalHeader>
      <SkeletonText $width="100px" $height="14px" />
      <SkeletonText $width="60px" $height="14px" />
    </GoalHeader>
    <SkeletonRect $width="100%" $height="8px" $radius="4px" />
  </GoalWrapper>
);

// Club Card Skeleton
const ClubCardWrapper = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.border};
`;

const ClubCover = styled.div`
  height: 140px;
  background: ${({ theme }) => theme.bgHover};
`;

const ClubContent = styled.div`
  padding: 16px 20px 20px;
`;

const ClubHeader = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
`;

export const SkeletonClubCard = () => (
  <ClubCardWrapper>
    <ClubCover>
      <SkeletonRect $width="100%" $height="140px" $radius="0" />
    </ClubCover>
    <ClubContent>
      <ClubHeader>
        <SkeletonText $width="60%" $height="18px" />
        <SkeletonText $width="60px" $height="24px" $radius="12px" />
      </ClubHeader>
      <SkeletonText $width="90%" $height="14px" $mb="8px" />
      <SkeletonText $width="70%" $height="14px" $mb="12px" />
      <SkeletonText $width="40%" $height="12px" $mb="16px" />
      <SkeletonRect $width="100%" $height="44px" $radius="8px" />
    </ClubContent>
  </ClubCardWrapper>
);

const Skeleton = {
  SkeletonText,
  SkeletonCircle,
  SkeletonRect,
  SkeletonActivityCard,
  SkeletonStatsCard,
  SkeletonEventCard,
  SkeletonChart,
  SkeletonGoalProgress,
  SkeletonClubCard,
};

export default Skeleton;
