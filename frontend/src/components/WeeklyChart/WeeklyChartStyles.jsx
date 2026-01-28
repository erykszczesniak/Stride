import styled from "styled-components";

export const ChartContainer = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
`;

export const ChartHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

export const ChartTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
`;

export const TrendBadge = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background-color: ${(props) => (props.positive ? "#f0fdf4" : "#fef2f2")};
  color: ${(props) => (props.positive ? "#22c55e" : "#ef4444")};
`;

export const ChartWrapper = styled.div`
  width: 100%;
  height: 200px;
`;

export const ChartLegend = styled.div`
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
`;

export const LegendItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
`;

export const LegendDot = styled.span`
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: ${(props) => props.color};
`;
