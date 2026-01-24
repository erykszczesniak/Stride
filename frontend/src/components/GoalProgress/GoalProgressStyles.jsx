import styled from "styled-components";

export const GoalCard = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const GoalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
`;

export const GoalTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
`;

export const GoalPeriod = styled.span`
  font-size: 12px;
  color: #666;
  background-color: #f5f5f5;
  padding: 4px 8px;
  border-radius: 4px;
`;

export const GoalType = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
`;

export const GoalIcon = styled.span`
  font-size: 20px;
`;

export const GoalLabel = styled.span`
  font-size: 14px;
  color: #666;
`;

export const ProgressContainer = styled.div`
  margin-bottom: 12px;
`;

export const ProgressBar = styled.div`
  width: 100%;
  height: 8px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background-color: ${(props) => (props.completed ? "#22c55e" : "#fc4c02")};
  border-radius: 4px;
  transition: width 0.3s ease;
  width: ${(props) => Math.min(props.progress, 100)}%;
`;

export const ProgressStats = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
`;

export const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: #333;
`;

export const ProgressTarget = styled.span`
  font-size: 13px;
  color: #666;
`;

export const GoalsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;
