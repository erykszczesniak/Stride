import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking, FaWalking, FaDumbbell } from "react-icons/fa";
import { FiArrowLeft, FiEdit2, FiTrash2, FiTarget } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import GoalService from "../api/GoalService";
import { useUnits } from "../context/UnitsContext";
import { LoadingSpinner } from "./DashboardStyles";
import {
  CreateGoalContainer,
  BackLink,
  CreateGoalTitle,
  FormCard,
  FormSection,
  FormSectionTitle,
  FormGroup,
  FormLabel,
  FormInput,
  OptionGrid,
  OptionButton,
  OptionIcon,
  OptionLabel,
  SubmitButton,
  ErrorMessage,
  UnitHint,
} from "./CreateGoalStyles";
import styled from "styled-components";

const activityIcons = {
  RUN: <FaRunning />,
  RIDE: <FaBicycle />,
  SWIM: <FaSwimmer />,
  HIKE: <FaHiking />,
  WALK: <FaWalking />,
  WEIGHT_TRAINING: <FaDumbbell />,
};

const goalTypeUnitKeys = { DISTANCE: "km", TIME: "hours", ELEVATION: "m", ACTIVITIES: "activities" };

const formatValue = (value, goalType, unitsCtx) => {
  if (goalType === "TIME") return (value / 3600).toFixed(1);
  if (goalType === "DISTANCE" && unitsCtx) return unitsCtx.convertDistance(value || 0).toFixed(1);
  if (goalType === "DISTANCE") return value?.toFixed(1);
  if (goalType === "ELEVATION" && unitsCtx) return Math.round(unitsCtx.convertElevation(value || 0));
  return Math.round(value || 0);
};

const DetailCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  margin-bottom: 24px;
`;

const DetailHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

const DetailTitle = styled.h2`
  font-size: 20px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  display: flex;
  align-items: center;
  gap: 12px;
`;

const DetailIcon = styled.span`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #fff7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fc4c02;
  font-size: 22px;
`;


const ProgressBarLarge = styled.div`
  width: 100%;
  height: 16px;
  background: #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  margin: 16px 0;
`;

const ProgressFillLarge = styled.div`
  height: 100%;
  background: ${({ $completed }) => $completed
    ? "linear-gradient(90deg, #22c55e 0%, #4ade80 100%)"
    : "linear-gradient(90deg, #fc4c02 0%, #ff6b35 100%)"};
  border-radius: 8px;
  transition: width 0.4s ease;
  width: ${({ $progress }) => Math.min($progress, 100)}%;
`;

const StatsRow = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 16px;
`;

const Stat = styled.div`
  text-align: center;
  padding: 16px;
  background: ${({ theme }) => theme.bgPrimary};
  border-radius: 12px;
`;

const StatValue = styled.div`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
`;

const StatLabel = styled.div`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 4px;
`;

const ButtonRow = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 24px;
`;

const EditButton = styled.button`
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  &:hover { background: ${({ theme }) => theme.bgHover}; }
`;

const ActionIcons = styled.div`
  display: flex;
  gap: 8px;
`;

const IconBtn = styled.button`
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  color: ${({ $danger, theme }) => $danger ? '#ef4444' : theme.textSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  &:hover {
    background: ${({ $danger }) => $danger ? '#fef2f2' : '#f1f5f9'};
    color: ${({ $danger }) => $danger ? '#dc2626' : '#1e293b'};
  }
`;

const ConfirmOverlay = styled.div`
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
`;

const ConfirmBox = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
`;

const ConfirmTitle = styled.h3`
  font-size: 18px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px;
`;

const ConfirmText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 24px;
`;

const ConfirmButtons = styled.div`
  display: flex;
  gap: 12px;
`;

const CancelBtn = styled.button`
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  font-weight: 600;
  cursor: pointer;
`;

const DeleteConfirmBtn = styled.button`
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  background: #ef4444;
  color: white;
  font-weight: 600;
  cursor: pointer;
  &:hover { background: #dc2626; }
`;

const GoalDetails = () => {
  const { t } = useTranslation();
  const { goalId } = useParams();
  const navigate = useNavigate();
  const unitsCtx = useUnits();

  const goalTypes = [
    { value: "DISTANCE", label: t("goals.types.DISTANCE"), icon: "📏" },
    { value: "TIME", label: t("goals.types.TIME"), icon: "⏱️" },
    { value: "ACTIVITIES", label: t("goals.types.ACTIVITIES"), icon: "🔢" },
    { value: "ELEVATION", label: t("goals.types.ELEVATION"), icon: "⛰️" },
  ];

  const activityTypes = [
    { value: "RUN", label: t("activities.typesShort.RUN"), icon: <FaRunning /> },
    { value: "RIDE", label: t("activities.typesShort.RIDE"), icon: <FaBicycle /> },
    { value: "SWIM", label: t("activities.typesShort.SWIM"), icon: <FaSwimmer /> },
    { value: "HIKE", label: t("activities.typesShort.HIKE"), icon: <FaHiking /> },
    { value: "WALK", label: t("activities.typesShort.WALK"), icon: <FaWalking /> },
    { value: "WEIGHT_TRAINING", label: t("activities.typesShort.WEIGHT_TRAINING"), icon: <FaDumbbell /> },
  ];

  const [goal, setGoal] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ goalType: "", activityType: "", targetValue: "" });

  useEffect(() => {
    const fetchGoal = async () => {
      try {
        const res = await GoalService.getGoalById(goalId);
        setGoal(res.data);
        setForm({
          goalType: res.data.goalType,
          activityType: res.data.activityType || "",
          targetValue: res.data.targetValue,
        });
      } catch {
        navigate("/goals");
      } finally {
        setLoading(false);
      }
    };
    fetchGoal();
  }, [goalId, navigate]);

  const handleDelete = async () => {
    await GoalService.deleteGoal(goalId);
    navigate("/goals");
  };

  const handleSave = async () => {
    setError("");
    if (!form.targetValue || parseFloat(form.targetValue) <= 0) {
      setError(t("goals.errors.targetMustBePositive"));
      return;
    }
    try {
      const res = await GoalService.updateGoal(goalId, {
        goalType: form.goalType,
        period: goal.period,
        activityType: form.activityType || null,
        targetValue: parseFloat(form.targetValue),
      });
      setGoal(res.data);
      setEditing(false);
    } catch (err) {
      setError(err.response?.data?.message || t("goals.errors.updateFailed"));
    }
  };

  if (loading) return <Layout><LoadingSpinner /></Layout>;
  if (!goal) return null;

  const progress = (goal.currentValue / goal.targetValue) * 100;
  const isCompleted = progress >= 100;
  const unit = goal.goalType === "DISTANCE"
    ? unitsCtx.distanceUnit
    : goal.goalType === "ELEVATION"
      ? unitsCtx.elevationUnit
      : goalTypeUnitKeys[goal.goalType] ? t(`goals.units.${goalTypeUnitKeys[goal.goalType]}`) : "";
  const icon = activityIcons[goal.activityType] || <FiTarget />;
  const periodLabel = goal.period === "WEEKLY" ? t("goals.weekly") : goal.period === "MONTHLY" ? t("goals.monthly") : t("goals.yearly");

  return (
    <Layout>
      <CreateGoalContainer>
        <BackLink to="/goals">
          <FiArrowLeft size={18} />
          {t("goals.backToGoals")}
        </BackLink>

        {!editing ? (
          <>
            <DetailCard>
              <DetailHeader>
                <DetailTitle>
                  <DetailIcon>{icon}</DetailIcon>
                  {periodLabel} {t(`goals.types.${goal.goalType}`)}
                </DetailTitle>
                <ActionIcons>
                  <IconBtn onClick={() => setEditing(true)} title={t("common.edit")}>
                    <FiEdit2 size={16} />
                  </IconBtn>
                  <IconBtn $danger onClick={() => setShowDelete(true)} title={t("common.delete")}>
                    <FiTrash2 size={16} />
                  </IconBtn>
                </ActionIcons>
              </DetailHeader>

              <ProgressBarLarge>
                <ProgressFillLarge $progress={progress} $completed={isCompleted} />
              </ProgressBarLarge>

              <StatsRow>
                <Stat>
                  <StatValue>{formatValue(goal.currentValue, goal.goalType, unitsCtx)}</StatValue>
                  <StatLabel>{t("goals.current", { unit })}</StatLabel>
                </Stat>
                <Stat>
                  <StatValue>{formatValue(goal.targetValue, goal.goalType, unitsCtx)}</StatValue>
                  <StatLabel>{t("goals.targetWith", { unit })}</StatLabel>
                </Stat>
                <Stat>
                  <StatValue>{Math.min(Math.round(progress), 100)}%</StatValue>
                  <StatLabel>{t("goals.progress")}</StatLabel>
                </Stat>
              </StatsRow>

              {goal.activityType && (
                <StatsRow style={{ gridTemplateColumns: "repeat(2, 1fr)" }}>
                  <Stat>
                    <StatValue>{t(`activities.typesShort.${goal.activityType}`, { defaultValue: goal.activityType })}</StatValue>
                    <StatLabel>{t("goals.activityTypeLabel")}</StatLabel>
                  </Stat>
                  <Stat>
                    <StatValue>{formatValue(goal.remainingValue, goal.goalType, unitsCtx)}</StatValue>
                    <StatLabel>{t("goals.remaining", { unit })}</StatLabel>
                  </Stat>
                </StatsRow>
              )}

            </DetailCard>
          </>
        ) : (
          <>
            <CreateGoalTitle>{t("goals.editGoal")}</CreateGoalTitle>
            <FormCard>
              <FormSection>
                <FormSectionTitle>{t("goals.goalType")}</FormSectionTitle>
                <OptionGrid>
                  {goalTypes.map((type) => (
                    <OptionButton
                      key={type.value}
                      type="button"
                      $selected={form.goalType === type.value}
                      onClick={() => setForm((f) => ({ ...f, goalType: type.value }))}
                    >
                      <OptionIcon>{type.icon}</OptionIcon>
                      <OptionLabel>{type.label}</OptionLabel>
                    </OptionButton>
                  ))}
                </OptionGrid>
              </FormSection>

              <FormSection>
                <FormSectionTitle>{t("goals.activityType")}</FormSectionTitle>
                <OptionGrid>
                  {activityTypes.map((type) => (
                    <OptionButton
                      key={type.value}
                      type="button"
                      $selected={form.activityType === type.value}
                      onClick={() => setForm((f) => ({ ...f, activityType: type.value }))}
                    >
                      <OptionIcon>{type.icon}</OptionIcon>
                      <OptionLabel>{type.label}</OptionLabel>
                    </OptionButton>
                  ))}
                </OptionGrid>
              </FormSection>

              <FormSection>
                <FormSectionTitle>{t("goals.target")}</FormSectionTitle>
                <FormGroup>
                  <FormLabel>{t("goals.newTargetValue")}</FormLabel>
                  <FormInput
                    type="number"
                    step="0.1"
                    min="0.1"
                    value={form.targetValue}
                    onChange={(e) => setForm((f) => ({ ...f, targetValue: e.target.value }))}
                  />
                  <UnitHint>{form.goalType === "DISTANCE" ? unitsCtx.distanceUnit : form.goalType === "ELEVATION" ? unitsCtx.elevationUnit : goalTypeUnitKeys[form.goalType] ? t(`goals.units.${goalTypeUnitKeys[form.goalType]}`) : ""}</UnitHint>
                </FormGroup>
              </FormSection>

              {error && <ErrorMessage>{error}</ErrorMessage>}

              <ButtonRow>
                <EditButton type="button" onClick={() => setEditing(false)}>{t("common.cancel")}</EditButton>
                <SubmitButton type="button" onClick={handleSave} style={{ flex: 1 }}>
                  {t("goals.saveChanges")}
                </SubmitButton>
              </ButtonRow>
            </FormCard>
          </>
        )}
      </CreateGoalContainer>

      {showDelete && (
        <ConfirmOverlay onClick={() => setShowDelete(false)}>
          <ConfirmBox onClick={(e) => e.stopPropagation()}>
            <ConfirmTitle>{t("goals.deleteTitle")}</ConfirmTitle>
            <ConfirmText>{t("goals.deleteTextWithProgress")}</ConfirmText>
            <ConfirmButtons>
              <CancelBtn onClick={() => setShowDelete(false)}>{t("common.cancel")}</CancelBtn>
              <DeleteConfirmBtn onClick={handleDelete}>{t("common.delete")}</DeleteConfirmBtn>
            </ConfirmButtons>
          </ConfirmBox>
        </ConfirmOverlay>
      )}
    </Layout>
  );
};

export default GoalDetails;
