import styled from "styled-components";

export const PageHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
`;

export const GoalsList = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
`;

export const ConfirmOverlay = styled.div`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
`;

export const ConfirmBox = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 32px;
  max-width: 400px;
  width: 90%;
  text-align: center;
`;

export const ConfirmTitle = styled.h3`
  font-size: 18px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px;
`;

export const ConfirmText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 24px;
`;

export const ConfirmButtons = styled.div`
  display: flex;
  gap: 12px;
`;

export const CancelBtn = styled.button`
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  font-weight: 600;
  cursor: pointer;
`;

export const DeleteConfirmBtn = styled.button`
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  background: #ef4444;
  color: white;
  font-weight: 600;
  cursor: pointer;
  &:hover {
    background: #dc2626;
  }
`;
