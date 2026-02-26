import styled from "styled-components";

export const GoalCard = styled.div`
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const GoalHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
`;

export const GoalTitle = styled.h3`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const GoalPeriod = styled.span`
  font-size: 11px;
  color: #64748b;
  background-color: ${({ theme }) => theme.bgCard};
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: 500;
`;

export const GoalType = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
`;

export const GoalIcon = styled.span`
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: ${({ theme }) => theme.bgCard};
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fc4c02;
  font-size: 16px;
`;

export const GoalLabel = styled.span`
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
`;

export const ProgressContainer = styled.div``;

export const ProgressBar = styled.div`
  width: 100%;
  height: 8px;
  background-color: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background: ${(props) =>
    props.completed
      ? "linear-gradient(90deg, #22c55e 0%, #4ade80 100%)"
      : "linear-gradient(90deg, #fc4c02 0%, #ff6b35 100%)"
  };
  border-radius: 4px;
  transition: width 0.4s ease;
  width: ${(props) => Math.min(props.progress, 100)}%;
`;

export const ProgressStats = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
`;

export const ProgressValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const ProgressTarget = styled.span`
  font-size: 12px;
  color: #94a3b8;
  font-weight: 500;
`;

export const GoalsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;
