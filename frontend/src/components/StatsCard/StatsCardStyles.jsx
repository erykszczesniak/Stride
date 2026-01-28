import styled from "styled-components";

export const Card = styled.div`
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
`;

export const CardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
`;

export const IconWrapper = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${(props) => props.bgColor || "#f0f0f0"};
  color: ${(props) => props.iconColor || "#333"};

  svg {
    width: 22px;
    height: 22px;
  }
`;

export const TrendBadge = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #f0fdf4;
  color: #22c55e;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
`;

export const Label = styled.p`
  font-size: 13px;
  color: #64748b;
  margin: 0 0 6px 0;
  font-weight: 500;
`;

export const Value = styled.div`
  display: flex;
  align-items: baseline;
  gap: 6px;
`;

export const ValueNumber = styled.span`
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1;
`;

export const ValueUnit = styled.span`
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
`;

export const Content = styled.div`
  flex: 1;
`;

export const Trend = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${(props) => (props.positive ? "#22c55e" : "#ef4444")};
  display: flex;
  align-items: center;
  gap: 4px;
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
`;
