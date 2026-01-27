import React, { useState } from "react";
import { FiTarget, FiCalendar, FiTrendingUp, FiZap, FiFileText, FiGrid, FiFile } from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import AiService from "../api/AiService";
import styled from "styled-components";

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
  color: #333;
  margin: 0;
`;

const PageSubtitle = styled.p`
  font-size: 14px;
  color: #666;
  margin: 4px 0 0 0;
`;

const AiBadge = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #fff3ed;
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
  background-color: #f5f5f5;
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
  color: ${(props) => (props.active ? "white" : "#666")};

  &:hover {
    color: ${(props) => (props.active ? "white" : "#333")};
  }
`;

const FormCard = styled.div`
  background: white;
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
  color: #333;
  margin: 0;
`;

const CardSubtitle = styled.p`
  font-size: 14px;
  color: #666;
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
  color: #333;
  margin-bottom: 8px;
`;

const Select = styled.select`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

const Input = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;

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
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
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
  background: white;
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
  color: #333;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const SectionText = styled.p`
  font-size: 14px;
  color: #666;
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
  color: #333;
`;

const TipItem = styled.div`
  background-color: #fff8f5;
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
  color: #333;
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
  color: #333;
  margin: 0;
`;

const WeekDistance = styled.span`
  font-size: 14px;
  color: #666;
`;

const WeekSubtitle = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
`;

const WorkoutCard = styled.div`
  background: white;
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
  background-color: #f5f5f5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #666;
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
  color: #666;
`;

const WorkoutDescription = styled.p`
  font-size: 13px;
  color: #666;
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
  const [activeTab, setActiveTab] = useState("goals");
  const [generating, setGenerating] = useState(false);

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
      alert("Please describe what you want to achieve");
      return;
    }
    setGenerating(true);
    try {
      const response = await AiService.generateGoal(goalType, timeframe, goalDescription);
      console.log("Goal API response:", response.data);
      const data = parseAiResponse(response.data);
      console.log("Parsed goal data:", data);
      setGeneratedGoal(data);
    } catch (error) {
      console.error("Error generating goal:", error);
      console.error("Error details:", error.message);
      alert("Failed to generate goal. Please try again.");
    } finally {
      setGenerating(false);
    }
  };

  const handleGeneratePlan = async () => {
    if (!targetDistance) {
      alert("Please enter a target distance");
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
      console.log("Plan API response:", response.data);
      const data = parseAiResponse(response.data);
      console.log("Parsed plan data:", data);
      setGeneratedPlan(data);
    } catch (error) {
      console.error("Error generating plan:", error);
      console.error("Error details:", error.message);
      alert("Failed to generate training plan. Please try again.");
    } finally {
      setGenerating(false);
    }
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
                <ExportButton><FiFileText size={14} /> TXT</ExportButton>
                <ExportButton><FiGrid size={14} /> XLSX</ExportButton>
                <ExportButton><FiFile size={14} /> PDF</ExportButton>
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
          active={activeTab === "goals"}
          onClick={() => setActiveTab("goals")}
        >
          <FiTarget size={16} /> Goal Setting
        </Tab>
        <Tab
          active={activeTab === "training"}
          variant="training"
          onClick={() => setActiveTab("training")}
        >
          <FiCalendar size={16} /> Training Plans
        </Tab>
      </TabsContainer>

      {activeTab === "goals" ? renderGoalSettingTab() : renderTrainingPlansTab()}
    </Layout>
  );
};

export default TrainingPlans;
