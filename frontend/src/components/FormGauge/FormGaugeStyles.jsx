import styled from "styled-components";

export const GaugeCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
`;

export const GaugeHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
`;

export const GaugeTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const StatusWord = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ $state }) =>
    $state === "fresh" ? "#16a34a" : $state === "fatigued" ? "#ef4444" : "#94a3b8"};
`;

export const GaugeSvgWrap = styled.div`
  color: ${({ theme }) => theme.textPrimary};
  max-width: 260px;
  margin: 0 auto;
`;

export const NumbersRow = styled.div`
  display: flex;
  justify-content: space-around;
  margin-top: 12px;
`;

export const NumberCell = styled.div`
  text-align: center;

  &:not(:first-child) {
    border-left: 1px solid ${({ theme }) => theme.border};
    padding-left: 16px;
  }
`;

export const NumberValue = styled.div`
  font-size: 26px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  font-variant-numeric: tabular-nums;
`;

export const NumberLabel = styled.div`
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 2px;
`;
