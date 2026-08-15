import styled, { keyframes } from "styled-components";

export const ChatSection = styled.section`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  padding: 24px;
  margin-top: 24px;
`;

export const ChatHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
`;

export const ChatTitle = styled.h2`
  font-size: 1.2rem;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const ClearButton = styled.button`
  margin-left: auto;
  background: none;
  border: none;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    color: ${({ theme }) => theme.error};
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
`;

export const MessageList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 460px;
  overflow-y: auto;
  padding: 4px;
`;

export const EmptyState = styled.div`
  text-align: center;
  color: ${({ theme }) => theme.textSecondary};
  padding: 24px 8px;
  font-size: 0.95rem;
`;

export const Bubble = styled.div`
  max-width: 85%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 0.95rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  align-self: ${({ $role }) => ($role === "USER" ? "flex-end" : "flex-start")};
  background: ${({ $role, theme }) =>
    $role === "USER" ? theme.primary : theme.bgHover};
  color: ${({ $role, theme }) =>
    $role === "USER" ? "#ffffff" : theme.textPrimary};
  border-bottom-right-radius: ${({ $role }) => ($role === "USER" ? "4px" : "14px")};
  border-bottom-left-radius: ${({ $role }) => ($role === "USER" ? "14px" : "4px")};
`;

const blink = keyframes`
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
`;

export const TypingIndicator = styled.div`
  align-self: flex-start;
  display: flex;
  gap: 4px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 14px;
  border-bottom-left-radius: 4px;

  span {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: ${({ theme }) => theme.textSecondary};
    animation: ${blink} 1.4s infinite both;
  }
  span:nth-child(2) {
    animation-delay: 0.2s;
  }
  span:nth-child(3) {
    animation-delay: 0.4s;
  }
`;

export const SuggestionRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
`;

export const SuggestionChip = styled.button`
  background: ${({ theme }) => theme.primaryLight};
  color: ${({ theme }) => theme.primary};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 16px;
  padding: 6px 14px;
  font-size: 0.85rem;
  cursor: pointer;

  &:hover {
    background: ${({ theme }) => theme.primary};
    color: #ffffff;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const InputRow = styled.form`
  display: flex;
  gap: 8px;
  margin-top: 16px;
`;

export const ChatInput = styled.input`
  flex: 1;
  padding: 12px 14px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 10px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 0.95rem;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
  }

  &:disabled {
    opacity: 0.6;
  }
`;

export const SendButton = styled.button`
  background: ${({ theme }) => theme.primary};
  color: #ffffff;
  border: none;
  border-radius: 10px;
  padding: 0 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.primaryHover};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const ErrorText = styled.div`
  color: ${({ theme }) => theme.error};
  font-size: 0.85rem;
  margin-top: 8px;
`;
