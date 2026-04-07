import styled from "styled-components";
import { Link } from "react-router-dom";

export const CreateGoalContainer = styled.div`
  max-width: 640px;
  margin: 0 auto;
`;

export const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 16px;

  &:hover {
    color: ${({ theme }) => theme.textPrimary};
  }
`;

export const CreateGoalTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 24px 0;
`;

export const FormCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
`;

export const FormSection = styled.div`
  margin-bottom: 28px;

  &:last-of-type {
    margin-bottom: 24px;
  }
`;

export const FormSectionTitle = styled.h3`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 12px 0;
`;

export const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

export const FormLabel = styled.label`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  font-weight: 500;
`;

export const FormSelect = styled.select`
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgPrimary};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 14px;
  outline: none;
  cursor: pointer;

  &:focus {
    border-color: #fc4c02;
  }
`;

export const FormInput = styled.input`
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgPrimary};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 16px;
  outline: none;

  &:focus {
    border-color: #fc4c02;
  }

  &::placeholder {
    color: ${({ theme }) => theme.textSecondary};
    opacity: 0.6;
  }
`;

export const UnitHint = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const OptionGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
`;

export const OptionButton = styled.button`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border-radius: 12px;
  border: 2px solid ${({ $selected, theme }) => ($selected ? "#fc4c02" : theme.border)};
  background: ${({ $selected }) => ($selected ? "#fc4c0210" : "transparent")};
  color: ${({ $selected, theme }) => ($selected ? "#fc4c02" : theme.textPrimary)};
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: #fc4c02;
  }
`;

export const OptionIcon = styled.span`
  font-size: 20px;
  display: flex;
  align-items: center;
`;

export const OptionLabel = styled.span`
  font-size: 12px;
  font-weight: 600;
`;

export const SubmitButton = styled.button`
  width: 100%;
  padding: 14px;
  border-radius: 12px;
  border: none;
  background: #fc4c02;
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover:not(:disabled) {
    background: #e04400;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

export const ErrorMessage = styled.p`
  color: ${({ theme }) => theme.error};
  font-size: 13px;
  margin: 0 0 16px 0;
  padding: 10px 14px;
  background: ${({ theme }) => theme.errorLight || "#fef2f2"};
  border-radius: 8px;
`;
