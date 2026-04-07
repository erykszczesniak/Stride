import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
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

const goalTypes = [
  { value: "DISTANCE", label: "Distance", unit: "km", icon: "📏" },
  { value: "TIME", label: "Time", unit: "hours", icon: "⏱️" },
  { value: "ACTIVITIES", label: "Activities", unit: "activities", icon: "🔢" },
  { value: "ELEVATION", label: "Elevation", unit: "m", icon: "⛰️" },
];

const periods = [
  { value: "WEEKLY", label: "Weekly" },
  { value: "MONTHLY", label: "Monthly" },
  { value: "YEARLY", label: "Yearly" },
];

const activityTypes = [
  { value: "RUN", label: "Run", icon: <FaRunning /> },
  { value: "RIDE", label: "Ride", icon: <FaBicycle /> },
  { value: "SWIM", label: "Swim", icon: <FaSwimmer /> },
  { value: "HIKE", label: "Hike", icon: <FaHiking /> },
  { value: "WALK", label: "Walk", icon: <FaWalking /> },
  { value: "WEIGHT_TRAINING", label: "Weights", icon: <FaDumbbell /> },
];

const CreateGoal = () => {
  const navigate = useNavigate();
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
      setError("Target value must be a positive number");
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
      setError(err.response?.data?.message || "Failed to create goal");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Layout>
      <CreateGoalContainer>
        <BackLink to="/goals">
          <FiArrowLeft size={18} />
          Back to Goals
        </BackLink>

        <CreateGoalTitle>Create Goal</CreateGoalTitle>

        <form onSubmit={handleSubmit}>
          <FormCard>
            <FormSection>
              <FormSectionTitle>What do you want to track?</FormSectionTitle>
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
              <FormSectionTitle>Activity Type</FormSectionTitle>
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
              <FormSectionTitle>Period</FormSectionTitle>
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
              <FormSectionTitle>Target</FormSectionTitle>
              <FormGroup>
                <FormLabel>
                  How {form.goalType === "ACTIVITIES" ? "many" : "much"}?
                </FormLabel>
                <FormInput
                  type="number"
                  step={form.goalType === "ACTIVITIES" ? "1" : "0.1"}
                  min="0.1"
                  placeholder={`e.g. ${form.goalType === "DISTANCE" ? "50" : form.goalType === "TIME" ? "10" : form.goalType === "ACTIVITIES" ? "5" : "500"}`}
                  value={form.targetValue}
                  onChange={(e) => setForm((f) => ({ ...f, targetValue: e.target.value }))}
                  required
                />
                <UnitHint>{selectedGoalType?.unit} per {form.period.toLowerCase().replace("ly", "")}</UnitHint>
              </FormGroup>
            </FormSection>

            {error && <ErrorMessage>{error}</ErrorMessage>}

            <SubmitButton type="submit" disabled={submitting}>
              {submitting ? "Creating..." : "Create Goal"}
            </SubmitButton>
          </FormCard>
        </form>
      </CreateGoalContainer>
    </Layout>
  );
};

export default CreateGoal;
