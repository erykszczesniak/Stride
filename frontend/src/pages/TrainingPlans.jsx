import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { FiTarget, FiCalendar, FiTrendingUp, FiZap, FiFileText, FiGrid, FiFile, FiClock, FiChevronRight, FiChevronDown, FiArrowLeft } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import AiService from "../api/AiService";
import TrainingPlanService from "../api/TrainingPlanService";
import { useToast } from "../context/ToastContext";
import { useUnits } from "../context/UnitsContext";
import * as XLSX from "xlsx";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";
import {
  PageHeader,
  HeaderLeft,
  PageIcon,
  TitleGroup,
  PageTitle,
  PageSubtitle,
  AiBadge,
  TabsContainer,
  Tab,
  FormCard,
  CardHeader,
  CardIcon,
  CardTitleGroup,
  CardTitle,
  CardSubtitle,
  FormRow,
  FormGroup,
  Label,
  Select,
  Input,
  Textarea,
  GenerateButton,
  ResultBanner,
  BannerHeader,
  BannerTitle,
  ExportButtons,
  ExportButton,
  BannerText,
  ResultCard,
  ResultSection,
  SectionTitle,
  SectionText,
  MilestoneList,
  MilestoneItem,
  MilestoneNumber,
  MilestoneText,
  TipItem,
  TipIcon,
  TipText,
  WeekSection,
  WeekHeader,
  WeekTitle,
  WeekDistance,
  WeekSubtitle,
  WorkoutCard,
  DayBadge,
  WorkoutInfo,
  WorkoutHeader,
  WorkoutType,
  WorkoutStats,
  WorkoutDescription,
  PlansGrid,
  PlanCard,
  PlanCardIcon,
  PlanLevelBadge,
  PlanCardTitle,
  PlanCardDescription,
  PlanCardMeta,
  PlanMetaItem,
  ViewPlanButton,
  PlansHeader,
  PlansTitle,
  PlansSubtitle,
  BackButton,
  PlanDetailHeader,
  PlanDetailBadge,
  PlanDetailTitle,
  PlanDetailDescription,
  PlanDetailMeta,
  PlanDetailMetaItem,
  DetailExportButtons,
  DetailExportButton,
  WeeklyPlanTitle,
  WeekAccordion,
  WeekAccordionHeader,
  WeekNumber,
  WeekInfo,
  WeekName,
  WeekDescription,
  WeekExpandIcon,
  WeekContent,
  DayWorkout,
  DayIcon,
  DayInfo,
  DayName,
  DayType,
  DayDescription,
  DayDuration,
  LoadingPlans,
} from "./TrainingPlansStyles";

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
  const { t } = useTranslation();
  const toast = useToast();
  const unitsCtx = useUnits();
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
        toast.error(t("training.loadFailed"));
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
      toast.warning(t("training.goals.describeRequired"));
      return;
    }
    setGenerating(true);
    try {
      const response = await AiService.generateGoal(goalType, timeframe, goalDescription);
      const data = parseAiResponse(response.data);
      setGeneratedGoal(data);
    } catch (error) {
      const details = error.response?.data?.message || error.message || String(error);
      toast.error(t("training.goals.generateFailed"), t("common.error"), details);
    } finally {
      setGenerating(false);
    }
  };

  const handleGeneratePlan = async () => {
    if (!targetDistance) {
      toast.warning(t("training.plan.targetRequired"));
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
      const details = error.response?.data?.message || error.message || String(error);
      toast.error(t("training.plan.generateFailed"), t("common.error"), details);
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
      content += `WEEK ${week.week} - Total: ${unitsCtx.convertDistance(week.totalDistance).toFixed(1)} ${unitsCtx.distanceUnit}\n`;
      content += `${week.description}\n\n`;

      (week.workouts || []).forEach((workout) => {
        content += `  ${workout.day}: ${workout.type.toUpperCase()}\n`;
        content += `    Distance: ${unitsCtx.convertDistance(workout.distance).toFixed(1)} ${unitsCtx.distanceUnit} | Duration: ${workout.duration} min\n`;
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
      ["Week", `Total Distance (${unitsCtx.distanceUnit})`, "Description"]
    ];
    (generatedPlan.weeks || []).forEach((week) => {
      summaryData.push([`Week ${week.week}`, unitsCtx.convertDistance(week.totalDistance).toFixed(1), week.description]);
    });
    const summarySheet = XLSX.utils.aoa_to_sheet(summaryData);
    XLSX.utils.book_append_sheet(workbook, summarySheet, "Summary");

    // Workouts sheet
    const workoutsData = [
      ["Week", "Day", "Type", `Distance (${unitsCtx.distanceUnit})`, "Duration (min)", "Description"]
    ];
    (generatedPlan.weeks || []).forEach((week) => {
      (week.workouts || []).forEach((workout) => {
        workoutsData.push([
          week.week,
          workout.day,
          workout.type,
          unitsCtx.convertDistance(workout.distance).toFixed(1),
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
      doc.text(`Week ${week.week} - ${unitsCtx.convertDistance(week.totalDistance).toFixed(1)} ${unitsCtx.distanceUnit}`, 14, yPos);
      yPos += 6;

      doc.setFontSize(10);
      doc.setTextColor(100, 100, 100);
      doc.text(week.description, 14, yPos);
      yPos += 8;

      // Workouts table
      const tableData = (week.workouts || []).map((w) => [
        w.day,
        w.type,
        `${unitsCtx.convertDistance(w.distance).toFixed(1)} ${unitsCtx.distanceUnit}`,
        `${w.duration} min`,
        w.description
      ]);

      autoTable(doc, {
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

      autoTable(doc, {
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
        <LoadingPlans>{t("training.loadingPlans")}</LoadingPlans>
      );
    }

    if (selectedPlan) {
      const plan = readyMadePlans.find(p => p.id === selectedPlan);
      if (!plan) return null;

      return (
        <>
          <BackButton onClick={() => { setSelectedPlan(null); setExpandedWeeks({}); }}>
            <FiArrowLeft size={16} />
            {t("training.backToPlans")}
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

          <WeeklyPlanTitle>{t("training.weeklyPlan")}</WeeklyPlanTitle>

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
          <PlansTitle>{t("training.readyMadeTitle")}</PlansTitle>
          <PlansSubtitle>{t("training.readyMadeSubtitle")}</PlansSubtitle>
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
                  {t("training.durationLabel", { value: plan.duration })}
                </PlanMetaItem>
                <PlanMetaItem>
                  <FiClock size={14} />
                  {t("training.frequencyLabel", { value: plan.frequency })}
                </PlanMetaItem>
                <PlanMetaItem>
                  <FiTarget size={14} />
                  {t("training.goalLabel", { value: plan.goal })}
                </PlanMetaItem>
              </PlanCardMeta>
              <ViewPlanButton>
                {t("training.viewPlan")} <FiChevronRight size={16} />
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
            <CardTitle>{t("training.goals.title")}</CardTitle>
            <CardSubtitle>{t("training.goals.subtitle")}</CardSubtitle>
          </CardTitleGroup>
        </CardHeader>

        <FormRow>
          <FormGroup noMargin>
            <Label>{t("training.goals.goalType")}</Label>
            <Select value={goalType} onChange={(e) => setGoalType(e.target.value)}>
              <option value="distance">{t("training.goals.typeDistance")}</option>
              <option value="time">{t("training.goals.typeTime")}</option>
              <option value="frequency">{t("training.goals.typeFrequency")}</option>
              <option value="race">{t("training.goals.typeRace")}</option>
            </Select>
          </FormGroup>
          <FormGroup noMargin>
            <Label>{t("training.goals.timeframe")}</Label>
            <Select value={timeframe} onChange={(e) => setTimeframe(e.target.value)}>
              <option value="week">{t("training.goals.tfWeek")}</option>
              <option value="month">{t("training.goals.tfMonth")}</option>
              <option value="3months">{t("training.goals.tf3Months")}</option>
              <option value="year">{t("training.goals.tfYear")}</option>
            </Select>
          </FormGroup>
        </FormRow>

        <FormGroup>
          <Label>{t("training.goals.whatLabel")}</Label>
          <Textarea
            placeholder={t("training.goals.whatPlaceholder")}
            value={goalDescription}
            onChange={(e) => setGoalDescription(e.target.value)}
          />
        </FormGroup>

        <GenerateButton onClick={handleGenerateGoal} disabled={generating}>
          <FiZap size={18} />
          {generating ? t("common.generating") : t("training.goals.generate")}
        </GenerateButton>
      </FormCard>

      {generatedGoal && (
        <>
          <ResultBanner>
            <BannerTitle>
              <FiTarget /> {t("training.goals.yourSmartGoal")}
            </BannerTitle>
            <BannerText>{generatedGoal.smartGoal}</BannerText>
          </ResultBanner>

          <ResultCard>
            <ResultSection>
              <SectionTitle>
                <FiTrendingUp color="#fc4c02" /> {t("training.goals.whyWorks")}
              </SectionTitle>
              <SectionText>{generatedGoal.analysis}</SectionText>
            </ResultSection>
          </ResultCard>

          <ResultCard>
            <ResultSection>
              <SectionTitle>
                <FiCalendar color="#fc4c02" /> {t("training.goals.keyMilestones")}
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
                <FiZap color="#fc4c02" /> {t("training.goals.actionTips")}
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
            <CardTitle>{t("training.plan.title")}</CardTitle>
            <CardSubtitle>{t("training.plan.subtitle")}</CardSubtitle>
          </CardTitleGroup>
        </CardHeader>

        <FormRow>
          <FormGroup noMargin>
            <Label>{t("training.plan.activityType")}</Label>
            <Select value={activityType} onChange={(e) => setActivityType(e.target.value)}>
              <option value="running">{t("training.plan.running")}</option>
              <option value="cycling">{t("training.plan.cycling")}</option>
              <option value="swimming">{t("training.plan.swimming")}</option>
            </Select>
          </FormGroup>
          <FormGroup noMargin>
            <Label>{t("training.plan.experienceLevel")}</Label>
            <Select value={experienceLevel} onChange={(e) => setExperienceLevel(e.target.value)}>
              <option value="beginner">{t("training.plan.beginner")}</option>
              <option value="intermediate">{t("training.plan.intermediate")}</option>
              <option value="advanced">{t("training.plan.advanced")}</option>
            </Select>
          </FormGroup>
        </FormRow>

        <FormRow>
          <FormGroup noMargin>
            <Label>{t("training.plan.targetDistance")}</Label>
            <Input
              type="number"
              placeholder={t("training.plan.targetDistancePlaceholder")}
              value={targetDistance}
              onChange={(e) => setTargetDistance(e.target.value)}
            />
          </FormGroup>
          <FormGroup noMargin>
            <Label>{t("training.plan.planDuration")}</Label>
            <Select value={planDuration} onChange={(e) => setPlanDuration(e.target.value)}>
              <option value="4">{t("training.plan.weeks4")}</option>
              <option value="8">{t("training.plan.weeks8")}</option>
              <option value="12">{t("training.plan.weeks12")}</option>
              <option value="16">{t("training.plan.weeks16")}</option>
            </Select>
          </FormGroup>
        </FormRow>

        <GenerateButton variant="training" onClick={handleGeneratePlan} disabled={generating}>
          <FiZap size={18} />
          {generating ? t("common.generating") : t("training.plan.generate")}
        </GenerateButton>
      </FormCard>

      {generatedPlan && (
        <>
          <ResultBanner variant="training">
            <BannerHeader>
              <BannerTitle>
                <FiTarget /> {t("training.plan.yourPlan")}
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
                <WeekTitle>{t("training.plan.week", { number: week.week })}</WeekTitle>
                <WeekDistance>{unitsCtx.convertDistance(week.totalDistance).toFixed(1)} {unitsCtx.distanceUnit}</WeekDistance>
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
                          {unitsCtx.convertDistance(workout.distance).toFixed(1)} {unitsCtx.distanceUnit} • {workout.duration} min
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
            <PageTitle>{t("training.title")}</PageTitle>
            <PageSubtitle>{t("training.subtitle")}</PageSubtitle>
          </TitleGroup>
        </HeaderLeft>
        <AiBadge>
          <FiZap /> {t("training.aiPowered")}
        </AiBadge>
      </PageHeader>

      <TabsContainer>
        <Tab
          active={activeTab === "ready-made"}
          onClick={() => { setActiveTab("ready-made"); setSelectedPlan(null); setExpandedWeeks({}); }}
        >
          <FiZap size={16} /> {t("training.tabs.readyMade")}
        </Tab>
        <Tab
          active={activeTab === "goals"}
          onClick={() => setActiveTab("goals")}
        >
          <FiTarget size={16} /> {t("training.tabs.goals")}
        </Tab>
        <Tab
          active={activeTab === "training"}
          variant="training"
          onClick={() => setActiveTab("training")}
        >
          <FiCalendar size={16} /> {t("training.tabs.aiGenerator")}
        </Tab>
      </TabsContainer>

      {activeTab === "ready-made" && renderReadyMadePlansTab()}
      {activeTab === "goals" && renderGoalSettingTab()}
      {activeTab === "training" && renderTrainingPlansTab()}
    </Layout>
  );
};

export default TrainingPlans;
