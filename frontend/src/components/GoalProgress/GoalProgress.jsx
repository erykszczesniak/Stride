import React from "react";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import { FiTarget, FiClock, FiTrendingUp } from "react-icons/fi";
import {
  GoalCard,
  GoalHeader,
  GoalTitle,
  GoalPeriod,
  GoalType,
  GoalIcon,
  GoalLabel,
  ProgressContainer,
  ProgressBar,
  ProgressFill,
  ProgressStats,
  ProgressValue,
  ProgressTarget,
} from "./GoalProgressStyles";

const activityIcons = {
  RUN: <FaRunning />,
  RIDE: <FaBicycle />,
  SWIM: <FaSwimmer />,
  HIKE: <FaHiking />,
};

const goalTypeLabels = {
  DISTANCE: "Distance",
  TIME: "Time",
  ELEVATION: "Elevation",
  ACTIVITIES: "Activities",
};

const goalTypeUnits = {
  DISTANCE: "km",
  TIME: "hours",
  ELEVATION: "m",
  ACTIVITIES: "activities",
};

const formatValue = (value, goalType) => {
  if (goalType === "TIME") {
    return (value / 3600).toFixed(1);
  }
  if (goalType === "DISTANCE") {
    return value.toFixed(1);
  }
  return Math.round(value);
};

const GoalProgress = ({ goal }) => {
  const progress = (goal.currentValue / goal.targetValue) * 100;
  const isCompleted = progress >= 100;
  const icon = activityIcons[goal.activityType] || <FiTarget />;
  const unit = goalTypeUnits[goal.goalType] || "";

  return (
    <GoalCard>
      <GoalHeader>
        <GoalTitle>Weekly Goal</GoalTitle>
        <GoalPeriod>{goal.period}</GoalPeriod>
      </GoalHeader>

      <GoalType>
        <GoalIcon>{icon}</GoalIcon>
        <GoalLabel>
          {goalTypeLabels[goal.goalType]} - {goal.activityType}
        </GoalLabel>
      </GoalType>

      <ProgressContainer>
        <ProgressBar>
          <ProgressFill progress={progress} completed={isCompleted} />
        </ProgressBar>
        <ProgressStats>
          <ProgressValue>
            {formatValue(goal.currentValue, goal.goalType)} {unit}
          </ProgressValue>
          <ProgressTarget>
            of {formatValue(goal.targetValue, goal.goalType)} {unit}
          </ProgressTarget>
        </ProgressStats>
      </ProgressContainer>
    </GoalCard>
  );
};

export default GoalProgress;
