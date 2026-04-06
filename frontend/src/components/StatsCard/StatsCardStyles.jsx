import styled from "styled-components";

export const Card = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px ${({ theme }) => theme.shadow};
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
  background-color: ${(props) => props.bgColor || props.theme.bgHover};
  color: ${(props) => props.iconColor || props.theme.textPrimary};

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
  background: ${({ $isPositive, theme }) => $isPositive ? theme.primaryLight : (theme.errorLight || '#fef2f2')};
  color: ${({ $isPositive, theme }) => $isPositive ? theme.success : theme.error};
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
`;

export const Label = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 6px 0;
  font-weight: 500;
  transition: color 0.3s ease;
`;

export const Value = styled.div`
  display: flex;
  align-items: baseline;
  gap: 6px;
`;

export const ValueNumber = styled.span`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1;
  transition: color 0.3s ease;
`;

export const ValueUnit = styled.span`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  font-weight: 500;
  transition: color 0.3s ease;
`;

export const Content = styled.div`
  flex: 1;
`;

export const Trend = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${(props) => (props.positive ? props.theme.success : props.theme.error)};
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
