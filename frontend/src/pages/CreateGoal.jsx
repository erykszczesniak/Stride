import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiArrowLeft } from "react-icons/fi";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking, FaWalking, FaDumbbell } from "react-icons/fa";
import Layout from "../components/Layout/Layout";
import GoalService from "../api/GoalService";
import {
  CreateGoalContainer,
  BackLink,
  CreateGoalTitle,
  FormCard,
  FormSection,
  FormSectionTitle,
  FormGroup,
  FormLabel,
  FormSelect,
  FormInput,
  OptionGrid,
  OptionButton,
  OptionIcon,
  OptionLabel,
  SubmitButton,
  ErrorMessage,
  UnitHint,
} from "./CreateGoalStyles";

const CreateGoal = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const goalTypes = [
    { value: "DISTANCE", label: t("goals.types.DISTANCE"), unit: t("goals.units.km"), icon: "📏" },
    { value: "TIME", label: t("goals.types.TIME"), unit: t("goals.units.hours"), icon: "⏱️" },
    { value: "ACTIVITIES", label: t("goals.types.ACTIVITIES"), unit: t("goals.units.activities"), icon: "🔢" },
    { value: "ELEVATION", label: t("goals.types.ELEVATION"), unit: t("goals.units.m"), icon: "⛰️" },
  ];

  const periods = [
    { value: "WEEKLY", label: t("goals.weekly") },
    { value: "MONTHLY", label: t("goals.monthly") },
    { value: "YEARLY", label: t("goals.yearly") },
  ];

  const activityTypes = [
    { value: "RUN", label: t("activities.typesShort.RUN"), icon: <FaRunning /> },
    { value: "RIDE", label: t("activities.typesShort.RIDE"), icon: <FaBicycle /> },
    { value: "SWIM", label: t("activities.typesShort.SWIM"), icon: <FaSwimmer /> },
    { value: "HIKE", label: t("activities.typesShort.HIKE"), icon: <FaHiking /> },
    { value: "WALK", label: t("activities.typesShort.WALK"), icon: <FaWalking /> },
    { value: "WEIGHT_TRAINING", label: t("activities.typesShort.WEIGHT_TRAINING"), icon: <FaDumbbell /> },
  ];
  const [form, setForm] = useState({
    goalType: "DISTANCE",
    period: "WEEKLY",
    activityType: "RUN",
    targetValue: "",
  });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const selectedGoalType = goalTypes.find((g) => g.value === form.goalType);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.targetValue || parseFloat(form.targetValue) <= 0) {
      setError(t("goals.errors.targetPositive"));
      return;
    }

    try {
      setSubmitting(true);
      await GoalService.createGoal({
        goalType: form.goalType,
        period: form.period,
        activityType: form.activityType,
        targetValue: parseFloat(form.targetValue),
      });
      navigate("/goals");
    } catch (err) {
      setError(err.response?.data?.message || t("goals.errors.createFailed"));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Layout>
      <CreateGoalContainer>
        <BackLink to="/goals">
          <FiArrowLeft size={18} />
          {t("goals.backToGoals")}
        </BackLink>

        <CreateGoalTitle>{t("goals.createGoal")}</CreateGoalTitle>

        <form onSubmit={handleSubmit}>
          <FormCard>
            <FormSection>
              <FormSectionTitle>{t("goals.whatToTrack")}</FormSectionTitle>
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
              <FormSectionTitle>{t("goals.period")}</FormSectionTitle>
              <FormGroup>
                <FormSelect
                  value={form.period}
                  onChange={(e) => setForm((f) => ({ ...f, period: e.target.value }))}
                >
                  {periods.map((p) => (
                    <option key={p.value} value={p.value}>
                      {p.label}
                    </option>
                  ))}
                </FormSelect>
              </FormGroup>
            </FormSection>

            <FormSection>
              <FormSectionTitle>{t("goals.target")}</FormSectionTitle>
              <FormGroup>
                <FormLabel>
                  {t("goals.howMuchMany", { question: form.goalType === "ACTIVITIES" ? t("goals.howMany") : t("goals.howMuch") })}
                </FormLabel>
                <FormInput
                  type="number"
                  step={form.goalType === "ACTIVITIES" ? "1" : "0.1"}
                  min="0.1"
                  placeholder={t("goals.placeholder", { example: form.goalType === "DISTANCE" ? "50" : form.goalType === "TIME" ? "10" : form.goalType === "ACTIVITIES" ? "5" : "500" })}
                  value={form.targetValue}
                  onChange={(e) => setForm((f) => ({ ...f, targetValue: e.target.value }))}
                  required
                />
                <UnitHint>{t("goals.unitPer", { unit: selectedGoalType?.unit || "", period: form.period.toLowerCase().replace("ly", "") })}</UnitHint>
              </FormGroup>
            </FormSection>

            {error && <ErrorMessage>{error}</ErrorMessage>}

            <SubmitButton type="submit" disabled={submitting}>
              {submitting ? t("common.creating") : t("goals.createGoal")}
            </SubmitButton>
          </FormCard>
        </form>
      </CreateGoalContainer>
    </Layout>
  );
};

export default CreateGoal;
