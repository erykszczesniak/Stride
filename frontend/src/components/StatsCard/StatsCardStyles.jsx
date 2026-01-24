import styled from "styled-components";

export const Card = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: flex-start;
  gap: 16px;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

export const IconWrapper = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background-color: ${(props) => props.bgColor || "#f0f0f0"};
  color: ${(props) => props.iconColor || "#333"};
`;

export const Content = styled.div`
  flex: 1;
`;

export const Label = styled.p`
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  font-weight: 500;
`;

export const Value = styled.h3`
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
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
