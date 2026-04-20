import styled from "styled-components";

export const ProfileSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const ProfileHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
`;

export const ProfileTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.3s ease;
  margin: 0;
`;

export const ProfileSummary = styled.div`
  display: flex;
  gap: 16px;
  font-size: 13px;
  font-weight: 500;
  color: ${({ theme }) => theme.textSecondary};

  /* Stack on narrow viewports so the gain/loss pair doesn't crowd the title. */
  @media (max-width: 600px) {
    gap: 12px;
    font-size: 12px;
  }
`;

export const GainValue = styled.span`
  color: #22c55e;
  font-weight: 600;
`;

export const LossValue = styled.span`
  color: #ef4444;
  font-weight: 600;
`;

export const ChartWrapper = styled.div`
  width: 100%;
  height: 200px;

  @media (max-width: 600px) {
    height: 140px;
  }
`;
