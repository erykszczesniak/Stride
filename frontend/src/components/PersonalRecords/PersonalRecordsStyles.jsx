import styled, { keyframes } from "styled-components";

const spin = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

export const PRContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const PRHeader = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
`;

export const PRTitleGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const PRTitle = styled.h2`
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 800;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;

  svg {
    color: #fc5200;
  }
`;

export const PRSubtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

export const RecalculateButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.textSecondary};
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;

  &:hover:not(:disabled) {
    border-color: #fc5200;
    color: #fc5200;
    background: rgba(252, 82, 0, 0.05);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .spinning {
    animation: ${spin} 1s linear infinite;
  }
`;

export const PREmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 60px 24px;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 16px;
  text-align: center;
`;

export const PREmptyIcon = styled.div`
  font-size: 64px;
  line-height: 1;
  opacity: 0.7;
`;

export const PREmptyText = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  max-width: 480px;
  line-height: 1.6;
  margin: 0;
`;

export const PRSportSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
`;

export const PRSportHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid ${({ theme }) => theme.borderLight || theme.border};
`;

export const PRSportIcon = styled.div`
  font-size: 32px;
  line-height: 1;
`;

export const PRSportName = styled.h3`
  flex: 1;
  font-size: 18px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const PRSportCount = styled.span`
  padding: 4px 12px;
  background: rgba(252, 82, 0, 0.1);
  color: #fc5200;
  font-size: 12px;
  font-weight: 700;
  border-radius: 12px;
`;

export const PRTable = styled.div`
  display: grid;
  grid-template-columns: 1.5fr 1fr 1fr 1.2fr 1fr;
  gap: 0;

  @media (max-width: 768px) {
    grid-template-columns: 1.4fr 1fr 0.8fr;
    & > *:nth-child(5n+4),
    & > *:nth-child(5n+5) {
      display: none;
    }
  }
`;

export const PRTableHeader = styled.div`
  display: contents;
`;

export const PRTableHeaderCell = styled.div`
  padding: 12px 16px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: ${({ theme }) => theme.textMuted};
  background: ${({ theme }) => theme.bgHover};

  &:first-child {
    border-top-left-radius: 8px;
  }

  &:last-child {
    border-top-right-radius: 8px;
  }
`;

export const PRTableRow = styled.div`
  display: contents;

  & > * {
    border-top: 1px solid ${({ theme }) => theme.borderLight || theme.border};
    transition: background 0.15s ease;
  }

  &:hover > * {
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const PRTableCell = styled.div`
  padding: 16px;
  display: flex;
  align-items: center;
`;

export const PRDistanceCell = styled(PRTableCell)`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  justify-content: center;
`;

export const PRDistanceBadge = styled.span`
  font-size: 22px;
  font-weight: 800;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1;

  small {
    font-size: 13px;
    font-weight: 600;
    color: ${({ theme }) => theme.textSecondary};
    margin-left: 2px;
  }
`;

export const PRDistanceLabel = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  font-weight: 500;
`;

export const PRTimeValue = styled.span`
  font-size: 18px;
  font-weight: 700;
  color: #fc5200;
  font-variant-numeric: tabular-nums;
`;

export const PRPaceValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  font-variant-numeric: tabular-nums;
`;

export const PRImprovementBadge = styled.span`
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
  border-radius: 12px;
  font-variant-numeric: tabular-nums;
`;

export const PRDateLink = styled.span`
  font-size: 13px;
  font-weight: 500;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  transition: color 0.15s ease;

  &:hover {
    color: #fc5200;
    text-decoration: underline;
  }
`;

export const PRLoadingState = styled.div`
  text-align: center;
  padding: 60px 24px;
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
`;
