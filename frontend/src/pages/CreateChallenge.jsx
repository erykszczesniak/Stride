import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FiArrowLeft, FiCalendar, FiTarget } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ChallengeService from "../api/ChallengeService";
import styled from "styled-components";

const PageHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
`;

const BackButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.textSecondary};
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
    color: ${({ theme }) => theme.textPrimary};
  }
`;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
`;

const FormCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
`;

const SectionTitle = styled.h2`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const FormRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
`;

const FormGroup = styled.div`
  margin-bottom: ${(props) => (props.noMargin ? "0" : "16px")};
`;

const Label = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

const Input = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};
  transition: border-color 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const TextArea = styled.textarea`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};
  min-height: 100px;
  resize: vertical;
  font-family: inherit;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const TypeGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
`;

const TypeButton = styled.button`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border: 2px solid
    ${(props) => (props.selected ? "#fc4c02" : props.theme.border)};
  background: ${(props) =>
    props.selected ? "rgba(252, 76, 2, 0.06)" : props.theme.bgCard};
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
  }
`;

const TypeIcon = styled.span`
  font-size: 28px;
`;

const TypeLabel = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
`;

const CheckboxGroup = styled.label`
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
`;

const Checkbox = styled.input`
  width: 18px;
  height: 18px;
  cursor: pointer;
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
`;

const CancelButton = styled.button`
  padding: 12px 24px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

const SaveButton = styled.button`
  padding: 12px 32px;
  border: none;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
`;

const ErrorMessage = styled.div`
  background: #ffebee;
  color: #c62828;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
`;

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
      console.error("Error creating challenge:", err);
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
