import styled from "styled-components";

export const ChartSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const ChartHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
`;

export const ChartTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.3s ease;
  margin: 0;
`;

export const ChartWrapper = styled.div`
  width: 100%;
  height: 220px;
`;
