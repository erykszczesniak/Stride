import React from "react";
import {
  SkeletonText,
  SkeletonCircle,
  SkeletonRect,
  ActivityCardWrapper,
  CardHeader,
  CardHeaderInfo,
  StatsRow,
  StatItem,
  CardFooter,
  StatsCardWrapper,
  StatsCardHeader,
  EventCardWrapper,
  EventDateBox,
  EventInfo,
  ChartWrapper,
  ChartHeader,
  ChartBars,
  GoalWrapper,
  GoalHeader,
  ClubCardWrapper,
  ClubCover,
  ClubContent,
  ClubHeader,
} from "./SkeletonStyles";

export { SkeletonText, SkeletonCircle, SkeletonRect };

// Activity Card Skeleton
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
