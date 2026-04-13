import React from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import { FiTarget, FiEdit2, FiTrash2 } from "react-icons/fi";
import { useUnits } from "../../context/UnitsContext";
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

const formatValue = (value, goalType, unitsCtx) => {
  if (goalType === "TIME") {
    return (value / 3600).toFixed(1);
  }
  if (goalType === "DISTANCE" && unitsCtx) {
    return unitsCtx.convertDistance(value).toFixed(1);
  }
  if (goalType === "DISTANCE") {
    return value.toFixed(1);
  }
  if (goalType === "ELEVATION" && unitsCtx) {
    return Math.round(unitsCtx.convertElevation(value));
  }
  return Math.round(value);
};

const GoalProgress = ({ goal, clickable = true, onEdit, onDelete }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const unitsCtx = useUnits();
  const progress = (goal.currentValue / goal.targetValue) * 100;
  const isCompleted = progress >= 100;
  const icon = activityIcons[goal.activityType] || <FiTarget />;
  const unit = goal.goalType === "DISTANCE"
    ? unitsCtx.distanceUnit
    : goal.goalType === "ELEVATION"
      ? unitsCtx.elevationUnit
      : t(`goals.units.${goal.goalType === "TIME" ? "hours" : "activities"}`);
  const handleClick = clickable ? () => navigate(`/goals/${goal.id}`) : undefined;
  const stopAndCall = (e, fn) => { e.stopPropagation(); fn(goal); };

  const periodLabelKey = goal.period === "WEEKLY" ? "weeklyGoal" : goal.period === "MONTHLY" ? "monthlyGoal" : "yearlyGoal";
  const activityTypeLabel = goal.activityType ? t(`activities.typesShort.${goal.activityType}`, { defaultValue: goal.activityType }) : "";

  return (
    <GoalCard onClick={handleClick} style={clickable ? { cursor: "pointer" } : undefined}>
      <GoalHeader>
        <GoalTitle>{t(`goals.${periodLabelKey}`)}</GoalTitle>
        <HeaderRight>
          {onEdit && <ActionBtn onClick={(e) => stopAndCall(e, onEdit)} title={t("common.edit")}><FiEdit2 size={18} /></ActionBtn>}
          {onDelete && <ActionBtn $danger onClick={(e) => stopAndCall(e, onDelete)} title={t("common.delete")}><FiTrash2 size={18} /></ActionBtn>}
          <GoalPeriod>{goal.period}</GoalPeriod>
        </HeaderRight>
      </GoalHeader>

      <GoalType>
        <GoalIcon>{icon}</GoalIcon>
        <GoalLabel>
          {t(`goals.types.${goal.goalType}`)} - {activityTypeLabel}
        </GoalLabel>
      </GoalType>

      <ProgressContainer>
        <ProgressBar>
          <ProgressFill progress={progress} completed={isCompleted} />
        </ProgressBar>
        <ProgressStats>
          <ProgressValue>
            {formatValue(goal.currentValue, goal.goalType, unitsCtx)} {unit}
          </ProgressValue>
          <ProgressTarget>
            {t("goals.ofTarget", { target: formatValue(goal.targetValue, goal.goalType, unitsCtx), unit })}
          </ProgressTarget>
        </ProgressStats>
      </ProgressContainer>
    </GoalCard>
  );
};

export default React.memo(GoalProgress);
