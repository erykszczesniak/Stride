import styled from "styled-components";

export const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
`;

export const HeaderLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const PageIcon = styled.div`
  font-size: 28px;
`;

export const TitleGroup = styled.div``;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const PageSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 4px 0 0 0;
`;

export const AiBadge = styled.div`
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

export const TabsContainer = styled.div`
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background-color: ${({ theme }) => theme.bgSecondary};
  padding: 4px;
  border-radius: 10px;
  width: fit-content;
`;

export const Tab = styled.button`
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

export const FormCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
`;

export const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
`;

export const CardIcon = styled.div`
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

export const CardTitleGroup = styled.div``;

export const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const CardSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 4px 0 0 0;
`;

export const FormRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
`;

export const FormGroup = styled.div`
  margin-bottom: ${(props) => (props.noMargin ? "0" : "16px")};
`;

export const Label = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

export const Select = styled.select`
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

export const Input = styled.input`
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

export const Textarea = styled.textarea`
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

export const GenerateButton = styled.button`
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

export const ResultBanner = styled.div`
  background: ${(props) =>
    props.variant === "training"
      ? "linear-gradient(135deg, #7b1fa2 0%, #e040fb 100%)"
      : "linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%)"};
  border-radius: 12px;
  padding: 20px 24px;
  color: white;
  margin-bottom: 24px;
`;

export const BannerHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
`;

export const BannerTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const ExportButtons = styled.div`
  display: flex;
  gap: 8px;
`;

export const ExportButton = styled.button`
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

export const BannerText = styled.p`
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
`;

export const ResultCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  margin-bottom: 16px;
`;

export const ResultSection = styled.div`
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const SectionTitle = styled.h4`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const SectionText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 0;
`;

export const MilestoneList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const MilestoneItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

export const MilestoneNumber = styled.span`
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

export const MilestoneText = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
`;

export const TipItem = styled.div`
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

export const TipIcon = styled.span`
  color: #fc4c02;
  flex-shrink: 0;
`;

export const TipText = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1.5;
`;

export const WeekSection = styled.div`
  margin-bottom: 24px;
`;

export const WeekHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

export const WeekTitle = styled.h4`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const WeekDistance = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const WeekSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 16px 0;
`;

export const WorkoutCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  gap: 16px;
`;

export const DayBadge = styled.div`
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

export const WorkoutInfo = styled.div`
  flex: 1;
`;

export const WorkoutHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
`;

export const WorkoutType = styled.span`
  background-color: ${(props) => props.bgColor || "#e8f5e9"};
  color: ${(props) => props.color || "#388e3c"};
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
`;

export const WorkoutStats = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const WorkoutDescription = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

// Ready-Made Plans Styled Components
export const PlansGrid = styled.div`
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

export const PlanCard = styled.div`
  background: ${props => props.gradient};
  border-radius: 16px;
  padding: 24px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  }
`;

export const PlanCardIcon = styled.div`
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

export const PlanLevelBadge = styled.span`
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

export const PlanCardTitle = styled.h3`
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px 0;
`;

export const PlanCardDescription = styled.p`
  font-size: 14px;
  opacity: 0.9;
  margin: 0 0 24px 0;
  line-height: 1.5;
`;

export const PlanCardMeta = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
  flex: 1;
`;

export const PlanMetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  opacity: 0.9;

  svg {
    opacity: 0.8;
  }
`;

export const ViewPlanButton = styled.button`
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

export const PlansHeader = styled.div`
  text-align: center;
  margin-bottom: 32px;
`;

export const PlansTitle = styled.h2`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

export const PlansSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

export const BackButton = styled.button`
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

export const PlanDetailHeader = styled.div`
  background: ${props => props.gradient};
  border-radius: 16px;
  padding: 24px;
  color: white;
  margin-bottom: 24px;
  position: relative;
`;

export const PlanDetailBadge = styled.span`
  display: inline-block;
  background: rgba(255, 255, 255, 0.9);
  color: ${props => props.color || '#333'};
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
`;

export const PlanDetailTitle = styled.h2`
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
`;

export const PlanDetailDescription = styled.p`
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 16px 0;
`;

export const PlanDetailMeta = styled.div`
  display: flex;
  gap: 24px;
`;

export const PlanDetailMetaItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
`;

export const DetailExportButtons = styled.div`
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const DetailExportButton = styled.button`
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

export const WeeklyPlanTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
`;

export const WeekAccordion = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
`;

export const WeekAccordionHeader = styled.div`
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const WeekNumber = styled.div`
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

export const WeekInfo = styled.div`
  flex: 1;
`;

export const WeekName = styled.div`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const WeekDescription = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 2px;
`;

export const WeekExpandIcon = styled.div`
  color: #9ca3af;
  transition: transform 0.2s;
  transform: ${props => props.expanded ? 'rotate(180deg)' : 'rotate(0)'};
`;

export const WeekContent = styled.div`
  padding: ${props => props.expanded ? '0 20px 20px 20px' : '0'};
  max-height: ${props => props.expanded ? '1000px' : '0'};
  overflow: hidden;
  transition: all 0.3s ease;
`;

export const DayWorkout = styled.div`
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

export const DayIcon = styled.div`
  color: #fc4c02;
  margin-right: 12px;
  margin-top: 2px;
`;

export const DayInfo = styled.div`
  flex: 1;
`;

export const DayName = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const DayType = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 2px;
`;

export const DayDescription = styled.div`
  font-size: 13px;
  color: #888;
  margin-top: 4px;
`;

export const DayDuration = styled.div`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  font-weight: 500;
  background: ${({ theme }) => theme.bgCard};
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid ${({ theme }) => theme.border};
`;

export const LoadingPlans = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 16px;
`;
