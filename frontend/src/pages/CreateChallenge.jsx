import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
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

const CreateChallenge = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const challengeTypes = [
    { type: "DISTANCE", label: t("challenges.types.DISTANCE"), icon: "📍", unit: t("goals.units.km") },
    { type: "TIME", label: t("challenges.types.TIME"), icon: "⏱️", unit: t("goals.units.hours") },
    { type: "ELEVATION", label: t("challenges.types.ELEVATION"), icon: "⛰️", unit: t("goals.units.m") },
    { type: "ACTIVITIES_COUNT", label: t("goals.types.ACTIVITIES"), icon: "📅", unit: t("common.days") },
  ];

  const activityTypes = [
    { type: "", label: t("activities.typesShort.ANY"), icon: "🏆" },
    { type: "RUN", label: t("activities.typesShort.RUN"), icon: "🏃" },
    { type: "RIDE", label: t("activities.typesShort.RIDE"), icon: "🚴" },
    { type: "SWIM", label: t("activities.typesShort.SWIM"), icon: "🏊" },
    { type: "HIKE", label: t("activities.typesShort.HIKE"), icon: "⛰️" },
    { type: "WALK", label: t("activities.typesShort.WALK"), icon: "🚶" },
  ];
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
      setError(t("challenges.create.errors.nameRequired"));
      return;
    }
    if (!formData.targetValue || parseFloat(formData.targetValue) <= 0) {
      setError(t("challenges.create.errors.targetInvalid"));
      return;
    }
    if (!formData.startDate || !formData.endDate) {
      setError(t("challenges.create.errors.datesRequired"));
      return;
    }
    if (new Date(formData.endDate) <= new Date(formData.startDate)) {
      setError(t("challenges.create.errors.endAfterStart"));
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
        err.response?.data?.error || t("challenges.create.errors.createFailed")
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
        <PageTitle>{t("challenges.createChallenge")}</PageTitle>
      </PageHeader>

      <form onSubmit={handleSubmit}>
        {error && <ErrorMessage>{error}</ErrorMessage>}

        <FormCard>
          <SectionTitle><FiTarget /> {t("challenges.create.challengeType")}</SectionTitle>
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
          <SectionTitle>{t("challenges.create.activityType")}</SectionTitle>
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
          <SectionTitle>{t("challenges.create.detailsSection")}</SectionTitle>
          <FormGroup>
            <Label>{t("challenges.create.name")}</Label>
            <Input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder={t("challenges.create.namePlaceholder")}
            />
          </FormGroup>

          <FormGroup>
            <Label>{t("challenges.create.description")}</Label>
            <TextArea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder={t("challenges.create.descriptionPlaceholder")}
            />
          </FormGroup>

          <FormGroup>
            <Label>{t("challenges.create.targetLabel", { unit: getUnit() })}</Label>
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
          <SectionTitle><FiCalendar /> {t("challenges.create.duration")}</SectionTitle>
          <FormRow>
            <FormGroup noMargin>
              <Label>{t("challenges.create.startDate")}</Label>
              <Input
                type="date"
                name="startDate"
                value={formData.startDate}
                onChange={handleInputChange}
              />
            </FormGroup>
            <FormGroup noMargin>
              <Label>{t("challenges.create.endDate")}</Label>
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
          <SectionTitle>{t("challenges.create.visibility")}</SectionTitle>
          <CheckboxGroup>
            <Checkbox
              type="checkbox"
              name="isPublic"
              checked={formData.isPublic}
              onChange={handleInputChange}
            />
            {t("challenges.create.publicLabel")}
          </CheckboxGroup>
        </FormCard>

        <ButtonGroup>
          <CancelButton type="button" onClick={() => navigate("/challenges")}>
            {t("common.cancel")}
          </CancelButton>
          <SaveButton type="submit" disabled={loading}>
            {loading ? t("common.creating") : t("challenges.create.createButton")}
          </SaveButton>
        </ButtonGroup>
      </form>
    </Layout>
  );
};

export default CreateChallenge;
