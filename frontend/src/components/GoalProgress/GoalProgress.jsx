import React from "react";
import { useNavigate } from "react-router-dom";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import { FiTarget, FiEdit2, FiTrash2 } from "react-icons/fi";
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
  HeaderRight,
  ActionBtn,
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

const GoalProgress = ({ goal, clickable = true, onEdit, onDelete }) => {
  const navigate = useNavigate();
  const progress = (goal.currentValue / goal.targetValue) * 100;
  const isCompleted = progress >= 100;
  const icon = activityIcons[goal.activityType] || <FiTarget />;
  const unit = goalTypeUnits[goal.goalType] || "";
  const handleClick = clickable ? () => navigate(`/goals/${goal.id}`) : undefined;
  const stopAndCall = (e, fn) => { e.stopPropagation(); fn(goal); };

  return (
    <GoalCard onClick={handleClick} style={clickable ? { cursor: "pointer" } : undefined}>
      <GoalHeader>
        <GoalTitle>{goal.period === "WEEKLY" ? "Weekly" : goal.period === "MONTHLY" ? "Monthly" : "Yearly"} Goal</GoalTitle>
        <HeaderRight>
          {onEdit && <ActionBtn onClick={(e) => stopAndCall(e, onEdit)} title="Edit"><FiEdit2 size={18} /></ActionBtn>}
          {onDelete && <ActionBtn $danger onClick={(e) => stopAndCall(e, onDelete)} title="Delete"><FiTrash2 size={18} /></ActionBtn>}
          <GoalPeriod>{goal.period}</GoalPeriod>
        </HeaderRight>
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

export default React.memo(GoalProgress);
