import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FiArrowLeft, FiCalendar, FiTarget } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ChallengeService from "../api/ChallengeService";
import {
  PageHeader,
  BackButton,
  PageTitle,
  FormCard,
  SectionTitle,
  FormRow,
  FormGroup,
  Label,
  Input,
  TextArea,
  TypeGrid,
  TypeButton,
  TypeIcon,
  TypeLabel,
  CheckboxGroup,
  Checkbox,
  ButtonGroup,
  CancelButton,
  SaveButton,
  ErrorMessage,
} from "./CreateChallengeStyles";

const challengeTypes = [
  { type: "DISTANCE", label: "Distance", icon: "📍", unit: "km" },
  { type: "TIME", label: "Time", icon: "⏱️", unit: "hours" },
  { type: "ELEVATION", label: "Elevation", icon: "⛰️", unit: "m" },
  { type: "ACTIVITIES_COUNT", label: "Activities", icon: "📅", unit: "days" },
];

const activityTypes = [
  { type: "", label: "Any", icon: "🏆" },
  { type: "RUN", label: "Run", icon: "🏃" },
  { type: "RIDE", label: "Ride", icon: "🚴" },
  { type: "SWIM", label: "Swim", icon: "🏊" },
  { type: "HIKE", label: "Hike", icon: "⛰️" },
  { type: "WALK", label: "Walk", icon: "🚶" },
];

const CreateChallenge = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedChallengeType, setSelectedChallengeType] = useState("DISTANCE");
  const [selectedActivityType, setSelectedActivityType] = useState("");
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    targetValue: "",
    startDate: "",
    endDate: "",
    isPublic: true,
  });

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const getUnit = () => {
    return challengeTypes.find((t) => t.type === selectedChallengeType)?.unit || "";
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!formData.name.trim()) {
      setError("Please enter a challenge name");
      return;
    }
    if (!formData.targetValue || parseFloat(formData.targetValue) <= 0) {
      setError("Please enter a valid target value");
      return;
    }
    if (!formData.startDate || !formData.endDate) {
      setError("Please enter start and end dates");
      return;
    }
    if (new Date(formData.endDate) <= new Date(formData.startDate)) {
      setError("End date must be after start date");
      return;
    }

    setLoading(true);

    try {
      const challengeData = {
        name: formData.name,
        description: formData.description || null,
        challengeType: selectedChallengeType,
        activityType: selectedActivityType || null,
        targetValue: parseFloat(formData.targetValue),
        startDate: `${formData.startDate}T00:00:00`,
        endDate: `${formData.endDate}T23:59:59`,
        isPublic: formData.isPublic,
      };

      await ChallengeService.createChallenge(challengeData);
      navigate("/challenges");
    } catch (err) {
      setError(
        err.response?.data?.error || "Failed to create challenge. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <PageHeader>
        <BackButton onClick={() => navigate("/challenges")}>
          <FiArrowLeft size={20} />
        </BackButton>
        <PageTitle>Create Challenge</PageTitle>
      </PageHeader>

      <form onSubmit={handleSubmit}>
        {error && <ErrorMessage>{error}</ErrorMessage>}

        <FormCard>
          <SectionTitle><FiTarget /> Challenge Type</SectionTitle>
          <TypeGrid>
            {challengeTypes.map((ct) => (
              <TypeButton
                key={ct.type}
                type="button"
                selected={selectedChallengeType === ct.type}
                onClick={() => setSelectedChallengeType(ct.type)}
              >
                <TypeIcon>{ct.icon}</TypeIcon>
                <TypeLabel>{ct.label}</TypeLabel>
              </TypeButton>
            ))}
          </TypeGrid>
        </FormCard>

        <FormCard>
          <SectionTitle>Activity Type</SectionTitle>
          <TypeGrid>
            {activityTypes.map((at) => (
              <TypeButton
                key={at.type}
                type="button"
                selected={selectedActivityType === at.type}
                onClick={() => setSelectedActivityType(at.type)}
              >
                <TypeIcon>{at.icon}</TypeIcon>
                <TypeLabel>{at.label}</TypeLabel>
              </TypeButton>
            ))}
          </TypeGrid>
        </FormCard>

        <FormCard>
          <SectionTitle>Details</SectionTitle>
          <FormGroup>
            <Label>Challenge Name *</Label>
            <Input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder="e.g., March Running Challenge"
            />
          </FormGroup>

          <FormGroup>
            <Label>Description</Label>
            <TextArea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="Describe the challenge and motivate participants..."
            />
          </FormGroup>

          <FormGroup>
            <Label>Target ({getUnit()}) *</Label>
            <Input
              type="number"
              name="targetValue"
              value={formData.targetValue}
              onChange={handleInputChange}
              placeholder={`e.g., ${selectedChallengeType === "DISTANCE" ? "100" : selectedChallengeType === "ELEVATION" ? "5000" : "20"}`}
              step="any"
              min="0"
            />
          </FormGroup>
        </FormCard>

        <FormCard>
          <SectionTitle><FiCalendar /> Duration</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>Start Date *</Label>
              <Input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleInputChange}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>End Date *</Label>
              <Input
                type="date"
                name="endDate"
                value={formData.endDate}
                onChange={handleInputChange}
              />
            </FormGroup>
          </FormRow>
        </FormCard>

        <FormCard>
          <SectionTitle>Visibility</SectionTitle>
          <CheckboxGroup>
            <Checkbox
              type="checkbox"
              name="isPublic"
              checked={formData.isPublic}
              onChange={handleInputChange}
            />
            Public challenge (visible to everyone)
          </CheckboxGroup>
        </FormCard>

        <ButtonGroup>
          <CancelButton type="button" onClick={() => navigate("/challenges")}>
            Cancel
          </CancelButton>
          <SaveButton type="submit" disabled={loading}>
            {loading ? "Creating..." : "Create Challenge"}
          </SaveButton>
        </ButtonGroup>
      </form>
    </Layout>
  );
};

export default CreateChallenge;
