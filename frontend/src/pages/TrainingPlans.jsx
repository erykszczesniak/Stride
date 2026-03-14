import React, { useState, useEffect } from "react";
import { FiTarget, FiCalendar, FiTrendingUp, FiZap, FiFileText, FiGrid, FiFile, FiClock, FiChevronRight, FiChevronDown, FiArrowLeft } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import AiService from "../api/AiService";
import TrainingPlanService from "../api/TrainingPlanService";
import { useToast } from "../context/ToastContext";
import styled from "styled-components";
import * as XLSX from "xlsx";
import jsPDF from "jspdf";
import "jspdf-autotable";

const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
`;

const HeaderLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

const PageIcon = styled.div`
  font-size: 28px;
`;

const TitleGroup = styled.div``;

const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

const PageSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 4px 0 0 0;
`;

const AiBadge = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: ${({ theme }) => theme.primaryLight};
  color: #fc4c02;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 14px;
`;

const TabsContainer = styled.div`
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background-color: ${({ theme }) => theme.bgSecondary};
  padding: 4px;
  border-radius: 10px;
  width: fit-content;
`;

const Tab = styled.button`
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: ${(props) => {
    if (!props.active) return "transparent";
    return props.variant === "training" ? "#e040fb" : "#fc4c02";
  }};
  color: ${(props) => (props.active ? "white" : props.theme.textSecondary)};

  &:hover {
    color: ${(props) => (props.active ? "white" : props.theme.textPrimary)};
  }
`;

const FormCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
`;

const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
`;

const CardIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background-color: ${(props) => props.bgColor || "#fff3ed"};
  color: ${(props) => props.color || "#fc4c02"};
`;

const CardTitleGroup = styled.div``;

const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

const CardSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 4px 0 0 0;
`;

const FormRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
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

const Select = styled.select`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgInput};
  cursor: pointer;
  color: ${({ theme }) => theme.textPrimary};

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

const Input = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const Textarea = styled.textarea`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgInput};
  min-height: 100px;
  resize: vertical;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }

  &::placeholder {
    color: #999;
  }
`;

const GenerateButton = styled.button`
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  background: ${(props) =>
    props.variant === "training"
      ? "linear-gradient(135deg, #e040fb 0%, #ff4081 100%)"
      : "linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%)"};
  color: white;

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

const ResultBanner = styled.div`
  background: ${(props) =>
    props.variant === "training"
      ? "linear-gradient(135deg, #7b1fa2 0%, #e040fb 100%)"
      : "linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%)"};
  border-radius: 12px;
  padding: 20px 24px;
  color: white;
  margin-bottom: 24px;
`;

const BannerHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
`;

const BannerTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const ExportButtons = styled.div`
  display: flex;
  gap: 8px;
`;

const ExportButton = styled.button`
  background: rgba(255, 255, 255, 0.2);
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  color: white;
  font-size: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
`;

const BannerText = styled.p`
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
`;

const ResultCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
`;

const ResultSection = styled.div`
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const SectionTitle = styled.h4`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const SectionText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 0;
`;

const MilestoneList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const MilestoneItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

const MilestoneNumber = styled.span`
  width: 24px;
  height: 24px;
  background-color: #fc4c02;
  color: white;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
`;

const MilestoneText = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
`;

const TipItem = styled.div`
  background-color: ${({ theme }) => theme.primaryLight};
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 8px;
  display: flex;
  align-items: flex-start;
  gap: 12px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const TipIcon = styled.span`
  color: #fc4c02;
  flex-shrink: 0;
`;

const TipText = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1.5;
`;

const WeekSection = styled.div`
  margin-bottom: 24px;
`;

const WeekHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

const WeekTitle = styled.h4`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

const WeekDistance = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
`;

const WeekSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 16px 0;
`;

const WorkoutCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
`;

const DayBadge = styled.div`
  width: 48px;
  height: 48px;
  background-color: ${({ theme }) => theme.bgSecondary};
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.textSecondary};
`;

const WorkoutInfo = styled.div`
  flex: 1;
`;

const WorkoutHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
`;

const WorkoutType = styled.span`
  background-color: ${(props) => props.bgColor || "#e8f5e9"};
  color: ${(props) => props.color || "#388e3c"};
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
`;

const WorkoutStats = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
`;

const WorkoutDescription = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

const getWorkoutTypeStyle = (type) => {
  const styles = {
    "easy run": { bgColor: "#e8f5e9", color: "#388e3c" },
    "tempo": { bgColor: "#fff3e0", color: "#f57c00" },
    "long run": { bgColor: "#e3f2fd", color: "#1976d2" },
    "interval": { bgColor: "#fce4ec", color: "#c2185b" },
    "rest": { bgColor: "#f5f5f5", color: "#666" },
    "recovery": { bgColor: "#f3e5f5", color: "#7b1fa2" },
  };
  return styles[type] || styles["easy run"];
};

// Ready-Made Plans Styled Components
const PlansGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
`;

const PlanCard = styled.div`
  background: ${props => props.gradient};
  border-radius: 16px;
  padding: 24px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  }
`;

const PlanCardIcon = styled.div`
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 16px;
`;

const PlanLevelBadge = styled.span`
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.9);
  color: ${props => props.color || '#333'};
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
`;

const PlanCardTitle = styled.h3`
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px 0;
`;

const PlanCardDescription = styled.p`
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 24px 0;
  line-height: 1.5;
`;

const PlanCardMeta = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
`;

const PlanMetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  opacity: 0.9;

  svg {
    opacity: 0.8;
  }
`;

const ViewPlanButton = styled.button`
  width: 100%;
  padding: 12px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 8px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
`;

const PlansHeader = styled.div`
  text-align: center;
  margin-bottom: 32px;
`;

const PlansTitle = styled.h2`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

const PlansSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

const BackButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 14px;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 16px;

  &:hover {
    color: ${({ theme }) => theme.textPrimary};
  }
`;

const PlanDetailHeader = styled.div`
  background: ${props => props.gradient};
  border-radius: 16px;
  padding: 24px;
  color: white;
  margin-bottom: 24px;
  position: relative;
`;

const PlanDetailBadge = styled.span`
  display: inline-block;
  background: rgba(255, 255, 255, 0.9);
  color: ${props => props.color || '#333'};
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
`;

const PlanDetailTitle = styled.h2`
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
`;

const PlanDetailDescription = styled.p`
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 16px 0;
`;

const PlanDetailMeta = styled.div`
  display: flex;
  gap: 24px;
`;

const PlanDetailMetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
`;

const DetailExportButtons = styled.div`
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

const DetailExportButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  background: ${props => props.variant === 'pdf' ? '#374151' : props.variant === 'excel' ? '#059669' : '#6b7280'};
  color: white;

  &:hover {
    opacity: 0.9;
    transform: translateX(-2px);
  }
`;

const WeeklyPlanTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
`;

const WeekAccordion = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
`;

const WeekAccordionHeader = styled.div`
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

const WeekNumber = styled.div`
  width: 36px;
  height: 36px;
  background: ${props => props.expanded ? '#fc4c02' : props.theme.bgSecondary};
  color: ${props => props.expanded ? 'white' : props.theme.textSecondary};
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  margin-right: 16px;
  transition: all 0.2s;
`;

const WeekInfo = styled.div`
  flex: 1;
`;

const WeekName = styled.div`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

const WeekDescription = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 2px;
`;

const WeekExpandIcon = styled.div`
  color: #9ca3af;
  transition: transform 0.2s;
  transform: ${props => props.expanded ? 'rotate(180deg)' : 'rotate(0)'};
`;

const WeekContent = styled.div`
  padding: ${props => props.expanded ? '0 20px 20px 20px' : '0'};
  max-height: ${props => props.expanded ? '1000px' : '0'};
  overflow: hidden;
  transition: all 0.3s ease;
`;

const DayWorkout = styled.div`
  display: flex;
  align-items: flex-start;
  padding: 16px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 8px;
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const DayIcon = styled.div`
  color: #fc4c02;
  margin-right: 12px;
  margin-top: 2px;
`;

const DayInfo = styled.div`
  flex: 1;
`;

const DayName = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

const DayType = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 2px;
`;

const DayDescription = styled.div`
  font-size: 13px;
  color: #888;
  margin-top: 4px;
`;

const DayDuration = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  font-weight: 500;
  background: ${({ theme }) => theme.bgCard};
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid ${({ theme }) => theme.border};
`;

const LoadingPlans = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 16px;
`;

const parseAiResponse = (data) => {
  // If already an object, return it directly
  if (typeof data === "object" && data !== null) {
    return data;
  }
  let text = String(data);
  // Remove markdown code blocks if present
  text = text.replace(/^```json\s*/i, "").replace(/\s*```$/i, "");
  text = text.replace(/^```\s*/i, "").replace(/\s*```$/i, "");
  return JSON.parse(text.trim());
};

const TrainingPlans = () => {
  const toast = useToast();
  const [activeTab, setActiveTab] = useState("ready-made");
  const [generating, setGenerating] = useState(false);

  // Ready-Made Plans state
  const [readyMadePlans, setReadyMadePlans] = useState([]);
  const [loadingPlans, setLoadingPlans] = useState(true);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const [expandedWeeks, setExpandedWeeks] = useState({});

  // Fetch templates from API
  useEffect(() => {
    const fetchTemplates = async () => {
      try {
        setLoadingPlans(true);
        const response = await TrainingPlanService.getTemplates();
        setReadyMadePlans(response.data);
      } catch (error) {
        // TODO: Add error reporting service
        toast.error("Failed to load training plans");
      } finally {
        setLoadingPlans(false);
      }
    };
    fetchTemplates();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Goal Setting state
  const [goalType, setGoalType] = useState("distance");
  const [timeframe, setTimeframe] = useState("month");
  const [goalDescription, setGoalDescription] = useState("");
  const [generatedGoal, setGeneratedGoal] = useState(null);

  // Training Plan state
  const [activityType, setActivityType] = useState("running");
  const [experienceLevel, setExperienceLevel] = useState("intermediate");
  const [targetDistance, setTargetDistance] = useState("");
  const [planDuration, setPlanDuration] = useState("4");
  const [generatedPlan, setGeneratedPlan] = useState(null);

  const handleGenerateGoal = async () => {
    if (!goalDescription.trim()) {
      toast.warning("Please describe what you want to achieve");
      return;
    }
    setGenerating(true);
    try {
      const response = await AiService.generateGoal(goalType, timeframe, goalDescription);
      const data = parseAiResponse(response.data);
      setGeneratedGoal(data);
    } catch (error) {
      // TODO: Add error reporting service
      toast.error("Failed to generate goal. Please try again.");
    } finally {
      setGenerating(false);
    }
  };

  const handleGeneratePlan = async () => {
    if (!targetDistance) {
      toast.warning("Please enter a target distance");
      return;
    }
    setGenerating(true);
    try {
      const response = await AiService.generateTrainingPlan(
        activityType,
        experienceLevel,
        targetDistance,
        planDuration
      );
      const data = parseAiResponse(response.data);
      setGeneratedPlan(data);
    } catch (error) {
      // TODO: Add error reporting service
      toast.error("Failed to generate training plan. Please try again.");
    } finally {
      setGenerating(false);
    }
  };

  const exportToTxt = () => {
    if (!generatedPlan) return;

    let content = `TRAINING PLAN\n`;
    content += `${"=".repeat(50)}\n\n`;
    content += `${generatedPlan.description}\n\n`;

    (generatedPlan.weeks || []).forEach((week) => {
      content += `${"─".repeat(50)}\n`;
      content += `WEEK ${week.week} - Total: ${week.totalDistance} km\n`;
      content += `${week.description}\n\n`;

      (week.workouts || []).forEach((workout) => {
        content += `  ${workout.day}: ${workout.type.toUpperCase()}\n`;
        content += `    Distance: ${workout.distance} km | Duration: ${workout.duration} min\n`;
        content += `    ${workout.description}\n\n`;
      });
    });

    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "training-plan.txt";
    link.click();
    URL.revokeObjectURL(url);
  };

  const exportToXlsx = () => {
    if (!generatedPlan) return;

    const workbook = XLSX.utils.book_new();

    // Summary sheet
    const summaryData = [
      ["Training Plan"],
      [generatedPlan.description],
      [],
      ["Week", "Total Distance (km)", "Description"]
    ];
    (generatedPlan.weeks || []).forEach((week) => {
      summaryData.push([`Week ${week.week}`, week.totalDistance, week.description]);
    });
    const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);
    XLSX.utils.book_append_sheet(workbook, summarySheet, "Summary");

    // Workouts sheet
    const workoutsData = [
      ["Week", "Day", "Type", "Distance (km)", "Duration (min)", "Description"]
    ];
    (generatedPlan.weeks || []).forEach((week) => {
      (week.workouts || []).forEach((workout) => {
        workoutsData.push([
          week.week,
          workout.day,
          workout.type,
          workout.distance,
          workout.duration,
          workout.description
        ]);
      });
    });
    const workoutsSheet = XLSX.utils.aoa_to_sheet(workoutsData);
    XLSX.utils.book_append_sheet(workbook, workoutsSheet, "Workouts");

    XLSX.writeFile(workbook, "training-plan.xlsx");
  };

  const exportToPdf = () => {
    if (!generatedPlan) return;

    const doc = new jsPDF();

    // Title
    doc.setFontSize(20);
    doc.setTextColor(124, 31, 162);
    doc.text("Training Plan", 14, 20);

    // Description
    doc.setFontSize(11);
    doc.setTextColor(60, 60, 60);
    const descLines = doc.splitTextToSize(generatedPlan.description, 180);
    doc.text(descLines, 14, 30);

    let yPos = 40 + descLines.length * 5;

    // Weeks
    (generatedPlan.weeks || []).forEach((week) => {
      if (yPos > 270) {
        doc.addPage();
        yPos = 20;
      }

      // Week header
      doc.setFontSize(14);
      doc.setTextColor(124, 31, 162);
      doc.text(`Week ${week.week} - ${week.totalDistance} km`, 14, yPos);
      yPos += 6;

      doc.setFontSize(10);
      doc.setTextColor(100, 100, 100);
      doc.text(week.description, 14, yPos);
      yPos += 8;

      // Workouts table
      const tableData = (week.workouts || []).map((w) => [
        w.day,
        w.type,
        `${w.distance} km`,
        `${w.duration} min`,
        w.description
      ]);

      doc.autoTable({
        startY: yPos,
        head: [["Day", "Type", "Distance", "Duration", "Description"]],
        body: tableData,
        theme: "striped",
        headStyles: { fillColor: [124, 31, 162] },
        margin: { left: 14 },
        tableWidth: 182
      });

      yPos = doc.lastAutoTable.finalY + 10;
    });

    doc.save("training-plan.pdf");
  };

  const toggleWeek = (weekNum) => {
    setExpandedWeeks(prev => ({
      ...prev,
      [weekNum]: !prev[weekNum]
    }));
  };

  const exportPlanToTxt = (plan) => {
    let content = `${plan.title.toUpperCase()}\n`;
    content += `${"=".repeat(50)}\n\n`;
    content += `Level: ${plan.level}\n`;
    content += `Duration: ${plan.duration}\n`;
    content += `Frequency: ${plan.frequency}\n`;
    content += `Goal: ${plan.goal}\n\n`;
    content += `${plan.description}\n\n`;

    plan.weeks.forEach((week) => {
      content += `${"─".repeat(50)}\n`;
      content += `${week.name} - ${week.description}\n\n`;

      week.workouts.forEach((workout) => {
        content += `  ${workout.day}: ${workout.type}\n`;
        content += `    Duration: ${workout.duration}\n`;
        content += `    ${workout.description}\n\n`;
      });
    });

    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `${plan.id}-training-plan.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const exportPlanToXlsx = (plan) => {
    const workbook = XLSX.utils.book_new();

    // Summary sheet
    const summaryData = [
      [plan.title],
      [`Level: ${plan.level}`],
      [`Duration: ${plan.duration}`],
      [`Frequency: ${plan.frequency}`],
      [`Goal: ${plan.goal}`],
      [],
      [plan.description],
      [],
      ["Week", "Description"]
    ];
    plan.weeks.forEach((week) => {
      summaryData.push([week.name, week.description]);
    });
    const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);
    XLSX.utils.book_append_sheet(workbook, summarySheet, "Summary");

    // Workouts sheet
    const workoutsData = [
      ["Week", "Day", "Type", "Duration", "Description"]
    ];
    plan.weeks.forEach((week) => {
      week.workouts.forEach((workout) => {
        workoutsData.push([
          week.name,
          workout.day,
          workout.type,
          workout.duration,
          workout.description
        ]);
      });
    });
    const workoutsSheet = XLSX.utils.aoa_to_sheet(workoutsData);
    XLSX.utils.book_append_sheet(workbook, workoutsSheet, "Workouts");

    XLSX.writeFile(workbook, `${plan.id}-training-plan.xlsx`);
  };

  const exportPlanToPdf = (plan) => {
    const doc = new jsPDF();

    // Title
    doc.setFontSize(20);
    doc.setTextColor(252, 76, 2);
    doc.text(plan.title, 14, 20);

    // Info
    doc.setFontSize(11);
    doc.setTextColor(100, 100, 100);
    doc.text(`Level: ${plan.level} | Duration: ${plan.duration} | Frequency: ${plan.frequency} | Goal: ${plan.goal}`, 14, 30);

    // Description
    doc.setTextColor(60, 60, 60);
    const descLines = doc.splitTextToSize(plan.description, 180);
    doc.text(descLines, 14, 40);

    let yPos = 50 + descLines.length * 5;

    // Weeks
    plan.weeks.forEach((week) => {
      if (yPos > 250) {
        doc.addPage();
        yPos = 20;
      }

      // Week header
      doc.setFontSize(14);
      doc.setTextColor(252, 76, 2);
      doc.text(`${week.name} - ${week.description}`, 14, yPos);
      yPos += 8;

      // Workouts table
      const tableData = week.workouts.map((w) => [
        w.day,
        w.type,
        w.duration,
        w.description
      ]);

      doc.autoTable({
        startY: yPos,
        head: [["Day", "Type", "Duration", "Description"]],
        body: tableData,
        theme: "striped",
        headStyles: { fillColor: [252, 76, 2] },
        margin: { left: 14 },
        tableWidth: 182
      });

      yPos = doc.lastAutoTable.finalY + 12;
    });

    doc.save(`${plan.id}-training-plan.pdf`);
  };

  const renderReadyMadePlansTab = () => {
    if (loadingPlans) {
      return (
        <LoadingPlans>Loading training plans...</LoadingPlans>
      );
    }

    if (selectedPlan) {
      const plan = readyMadePlans.find(p => p.id === selectedPlan);
      if (!plan) return null;

      return (
        <>
          <BackButton onClick={() => { setSelectedPlan(null); setExpandedWeeks({}); }}>
            <FiArrowLeft size={16} />
            Back to plans
          </BackButton>

          <PlanDetailHeader gradient={plan.gradient}>
            <PlanDetailBadge color={plan.levelColor}>{plan.level}</PlanDetailBadge>
            <PlanDetailTitle>{plan.title}</PlanDetailTitle>
            <PlanDetailDescription>{plan.description}</PlanDetailDescription>
            <PlanDetailMeta>
              <PlanDetailMetaItem>
                <FiCalendar size={16} />
                {plan.duration}
              </PlanDetailMetaItem>
              <PlanDetailMetaItem>
                <FiClock size={16} />
                {plan.frequency}
              </PlanDetailMetaItem>
              <PlanDetailMetaItem>
                <FiTarget size={16} />
                {plan.goal}
              </PlanDetailMetaItem>
            </PlanDetailMeta>
            <DetailExportButtons>
              <DetailExportButton variant="pdf" onClick={() => exportPlanToPdf(plan)}>
                <FiFile size={14} /> PDF
              </DetailExportButton>
              <DetailExportButton variant="excel" onClick={() => exportPlanToXlsx(plan)}>
                <FiGrid size={14} /> Excel
              </DetailExportButton>
              <DetailExportButton variant="txt" onClick={() => exportPlanToTxt(plan)}>
                <FiFileText size={14} /> TXT
              </DetailExportButton>
            </DetailExportButtons>
          </PlanDetailHeader>

          <WeeklyPlanTitle>Weekly Plan</WeeklyPlanTitle>

          {plan.weeks.map((week) => (
            <WeekAccordion key={week.week}>
              <WeekAccordionHeader onClick={() => toggleWeek(week.week)}>
                <WeekNumber expanded={expandedWeeks[week.week]}>{week.week}</WeekNumber>
                <WeekInfo>
                  <WeekName>{week.name}</WeekName>
                  <WeekDescription>{week.description}</WeekDescription>
                </WeekInfo>
                <WeekExpandIcon expanded={expandedWeeks[week.week]}>
                  <FiChevronDown size={20} />
                </WeekExpandIcon>
              </WeekAccordionHeader>
              <WeekContent expanded={expandedWeeks[week.week]}>
                {week.workouts.map((workout, index) => (
                  <DayWorkout key={index}>
                    <DayIcon>🔥</DayIcon>
                    <DayInfo>
                      <DayName>{workout.day}</DayName>
                      <DayType>{workout.type}</DayType>
                      <DayDescription>{workout.description}</DayDescription>
                    </DayInfo>
                    <DayDuration>{workout.duration}</DayDuration>
                  </DayWorkout>
                ))}
              </WeekContent>
            </WeekAccordion>
          ))}
        </>
      );
    }

    return (
      <>
        <PlansHeader>
          <PlansTitle>Ready-Made Training Plans</PlansTitle>
          <PlansSubtitle>Choose a plan tailored to your level</PlansSubtitle>
        </PlansHeader>

        <PlansGrid>
          {readyMadePlans.map((plan) => (
            <PlanCard key={plan.id} gradient={plan.gradient} onClick={() => setSelectedPlan(plan.id)}>
              <PlanCardIcon>{plan.icon}</PlanCardIcon>
              <PlanLevelBadge color={plan.levelColor}>{plan.level}</PlanLevelBadge>
              <PlanCardTitle>{plan.title}</PlanCardTitle>
              <PlanCardDescription>{plan.description}</PlanCardDescription>
              <PlanCardMeta>
                <PlanMetaItem>
                  <FiCalendar size={14} />
                  Duration: {plan.duration}
                </PlanMetaItem>
                <PlanMetaItem>
                  <FiClock size={14} />
                  Frequency: {plan.frequency}
                </PlanMetaItem>
                <PlanMetaItem>
                  <FiTarget size={14} />
                  Goal: {plan.goal}
                </PlanMetaItem>
              </PlanCardMeta>
              <ViewPlanButton>
                View Plan <FiChevronRight size={16} />
              </ViewPlanButton>
            </PlanCard>
          ))}
        </PlansGrid>
      </>
    );
  };

  const renderGoalSettingTab = () => (
    <>
      <FormCard>
        <CardHeader>
          <CardIcon bgColor="#fff3ed" color="#fc4c02">
            <FiTarget />
          </CardIcon>
          <CardTitleGroup>
            <CardTitle>Set Your Goal</CardTitle>
            <CardSubtitle>AI will help you create a SMART goal</CardSubtitle>
          </CardTitleGroup>
        </CardHeader>

        <FormRow>
          <FormGroup noMargin>
            <Label>Goal Type</Label>
            <Select value={goalType} onChange={(e) => setGoalType(e.target.value)}>
              <option value="distance">Distance</option>
              <option value="time">Training Time</option>
              <option value="frequency">Activity Frequency</option>
              <option value="race">Race Preparation</option>
            </Select>
          </FormGroup>
          <FormGroup noMargin>
            <Label>Timeframe</Label>
            <Select value={timeframe} onChange={(e) => setTimeframe(e.target.value)}>
              <option value="week">This Week</option>
              <option value="month">This Month</option>
              <option value="3months">3 Months</option>
              <option value="year">This Year</option>
            </Select>
          </FormGroup>
        </FormRow>

        <FormGroup>
          <Label>What do you want to achieve?</Label>
          <Textarea
            placeholder="E.g., I want to run my first 10K, lose weight, improve my endurance..."
            value={goalDescription}
            onChange={(e) => setGoalDescription(e.target.value)}
          />
        </FormGroup>

        <GenerateButton onClick={handleGenerateGoal} disabled={generating}>
          <FiZap size={18} />
          {generating ? "Generating..." : "Generate My Goal"}
        </GenerateButton>
      </FormCard>

      {generatedGoal && (
        <>
          <ResultBanner>
            <BannerTitle>
              <FiTarget /> Your SMART Goal
            </BannerTitle>
            <BannerText>{generatedGoal.smartGoal}</BannerText>
          </ResultBanner>

          <ResultCard>
            <ResultSection>
              <SectionTitle>
                <FiTrendingUp color="#fc4c02" /> Why This Works For You
              </SectionTitle>
              <SectionText>{generatedGoal.analysis}</SectionText>
            </ResultSection>
          </ResultCard>

          <ResultCard>
            <ResultSection>
              <SectionTitle>
                <FiCalendar color="#fc4c02" /> Key Milestones
              </SectionTitle>
              <MilestoneList>
                {(generatedGoal.milestones || []).map((milestone, index) => (
                  <MilestoneItem key={index}>
                    <MilestoneNumber>{index + 1}</MilestoneNumber>
                    <MilestoneText>{milestone}</MilestoneText>
                  </MilestoneItem>
                ))}
              </MilestoneList>
            </ResultSection>
          </ResultCard>

          <ResultCard>
            <ResultSection>
              <SectionTitle>
                <FiZap color="#fc4c02" /> Action Tips
              </SectionTitle>
              {(generatedGoal.tips || []).map((tip, index) => (
                <TipItem key={index}>
                  <TipIcon><FiZap size={16} /></TipIcon>
                  <TipText>{tip}</TipText>
                </TipItem>
              ))}
            </ResultSection>
          </ResultCard>
        </>
      )}
    </>
  );

  const renderTrainingPlansTab = () => (
    <>
      <FormCard>
        <CardHeader>
          <CardIcon bgColor="#f3e5f5" color="#9c27b0">
            <FiCalendar />
          </CardIcon>
          <CardTitleGroup>
            <CardTitle>Generate Training Plan</CardTitle>
            <CardSubtitle>AI-powered personalized schedule</CardSubtitle>
          </CardTitleGroup>
        </CardHeader>

        <FormRow>
          <FormGroup noMargin>
            <Label>Activity Type</Label>
            <Select value={activityType} onChange={(e) => setActivityType(e.target.value)}>
              <option value="running">Running</option>
              <option value="cycling">Cycling</option>
              <option value="swimming">Swimming</option>
            </Select>
          </FormGroup>
          <FormGroup noMargin>
            <Label>Experience Level</Label>
            <Select value={experienceLevel} onChange={(e) => setExperienceLevel(e.target.value)}>
              <option value="beginner">Beginner</option>
              <option value="intermediate">Intermediate</option>
              <option value="advanced">Advanced</option>
            </Select>
          </FormGroup>
        </FormRow>

        <FormRow>
          <FormGroup noMargin>
            <Label>Target Distance (km)</Label>
            <Input
              type="number"
              placeholder="e.g., 10"
              value={targetDistance}
              onChange={(e) => setTargetDistance(e.target.value)}
            />
          </FormGroup>
          <FormGroup noMargin>
            <Label>Plan Duration</Label>
            <Select value={planDuration} onChange={(e) => setPlanDuration(e.target.value)}>
              <option value="4">4 Weeks</option>
              <option value="8">8 Weeks</option>
              <option value="12">12 Weeks</option>
              <option value="16">16 Weeks</option>
            </Select>
          </FormGroup>
        </FormRow>

        <GenerateButton variant="training" onClick={handleGeneratePlan} disabled={generating}>
          <FiZap size={18} />
          {generating ? "Generating..." : "Generate Training Plan"}
        </GenerateButton>
      </FormCard>

      {generatedPlan && (
        <>
          <ResultBanner variant="training">
            <BannerHeader>
              <BannerTitle>
                <FiTarget /> Your Training Plan
              </BannerTitle>
              <ExportButtons>
                <ExportButton onClick={exportToTxt}><FiFileText size={14} /> TXT</ExportButton>
                <ExportButton onClick={exportToXlsx}><FiGrid size={14} /> XLSX</ExportButton>
                <ExportButton onClick={exportToPdf}><FiFile size={14} /> PDF</ExportButton>
              </ExportButtons>
            </BannerHeader>
            <BannerText>{generatedPlan.description}</BannerText>
          </ResultBanner>

          {(generatedPlan.weeks || []).map((week) => (
            <WeekSection key={week.week}>
              <WeekHeader>
                <WeekTitle>Week {week.week}</WeekTitle>
                <WeekDistance>{week.totalDistance} km</WeekDistance>
              </WeekHeader>
              <WeekSubtitle>{week.description}</WeekSubtitle>

              {(week.workouts || []).map((workout, index) => {
                const typeStyle = getWorkoutTypeStyle(workout.type);
                return (
                  <WorkoutCard key={index}>
                    <DayBadge>{workout.day}</DayBadge>
                    <WorkoutInfo>
                      <WorkoutHeader>
                        <WorkoutType bgColor={typeStyle.bgColor} color={typeStyle.color}>
                          {workout.type}
                        </WorkoutType>
                        <WorkoutStats>
                          {workout.distance} km • {workout.duration} min
                        </WorkoutStats>
                      </WorkoutHeader>
                      <WorkoutDescription>{workout.description}</WorkoutDescription>
                    </WorkoutInfo>
                  </WorkoutCard>
                );
              })}
            </WeekSection>
          ))}
        </>
      )}
    </>
  );

  return (
    <Layout>
      <PageHeader>
        <HeaderLeft>
          <PageIcon>🧠</PageIcon>
          <TitleGroup>
            <PageTitle>Training Plans</PageTitle>
            <PageSubtitle>AI-powered goals and personalized training plans</PageSubtitle>
          </TitleGroup>
        </HeaderLeft>
        <AiBadge>
          <FiZap /> AI Powered
        </AiBadge>
      </PageHeader>

      <TabsContainer>
        <Tab
          active={activeTab === "ready-made"}
          onClick={() => { setActiveTab("ready-made"); setSelectedPlan(null); setExpandedWeeks({}); }}
        >
          <FiZap size={16} /> Ready-Made Plans
        </Tab>
        <Tab
          active={activeTab === "goals"}
          onClick={() => setActiveTab("goals")}
        >
          <FiTarget size={16} /> Goals
        </Tab>
        <Tab
          active={activeTab === "training"}
          variant="training"
          onClick={() => setActiveTab("training")}
        >
          <FiCalendar size={16} /> AI Generator
        </Tab>
      </TabsContainer>

      {activeTab === "ready-made" && renderReadyMadePlansTab()}
      {activeTab === "goals" && renderGoalSettingTab()}
      {activeTab === "training" && renderTrainingPlansTab()}
    </Layout>
  );
};

export default TrainingPlans;
