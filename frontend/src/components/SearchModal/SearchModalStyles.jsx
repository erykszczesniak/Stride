import styled, { keyframes } from "styled-components";
import { media } from "../../styles/theme";

const fadeIn = keyframes`
  from { opacity: 0; }
  to { opacity: 1; }
`;

const slideDown = keyframes`
  from {
    opacity: 0;
    transform: translateY(-16px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
`;

export const Overlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 120px;
  animation: ${fadeIn} 0.15s ease-out;

  ${media.sm} {
    padding-top: 80px;
  }
`;

export const ModalContainer = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.border};
  box-shadow: 0 16px 48px ${({ theme }) => theme.shadowMedium},
    0 0 0 1px ${({ theme }) => theme.border};
  width: 560px;
  height: 480px;
  min-width: 380px;
  min-height: 300px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 160px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  resize: both;
  animation: ${slideDown} 0.15s ease-out;
`;

export const SearchInputWrapper = styled.div`
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
  gap: 12px;

  svg {
    color: ${({ theme }) => theme.textMuted};
    flex-shrink: 0;
  }
`;

export const SearchInput = styled.input`
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 16px;
  color: ${({ theme }) => theme.textPrimary};
  font-family: inherit;

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
  }
`;

export const ShortcutHint = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textMuted};
  background: ${({ theme }) => theme.bgHover};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 4px;
  padding: 2px 6px;
  white-space: nowrap;
  flex-shrink: 0;
`;

export const ResultsList = styled.div`
  overflow-y: auto;
  padding: 8px;
  flex: 1;
`;

export const CategoryLabel = styled.div`
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: ${({ theme }) => theme.textMuted};
  padding: 8px 8px 4px 8px;
  margin-top: ${({ $first }) => ($first ? "0" : "8px")};
`;

export const ResultItem = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.1s ease;
  background: ${({ $active, theme }) =>
    $active ? theme.bgHover : "transparent"};

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const ResultIcon = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: ${({ $active, theme }) =>
    $active ? theme.primaryLight : theme.bgHover};
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: ${({ $active, theme }) =>
    $active ? theme.primary : theme.textSecondary};
  transition: all 0.1s ease;
`;

export const ResultLabel = styled.span`
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
`;

export const ResultCategory = styled.span`
  font-size: 11px;
  color: ${({ theme }) => theme.textMuted};
  background: ${({ theme }) => theme.bgHover};
  border-radius: 4px;
  padding: 2px 8px;
  white-space: nowrap;
`;

export const EmptyState = styled.div`
  padding: 32px 16px;
  text-align: center;
  color: ${({ theme }) => theme.textMuted};
  font-size: 14px;
`;

export const Footer = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  border-top: 1px solid ${({ theme }) => theme.border};
  font-size: 11px;
  color: ${({ theme }) => theme.textMuted};
`;

export const FooterHint = styled.span`
  display: flex;
  align-items: center;
  gap: 4px;

  kbd {
    background: ${({ theme }) => theme.bgHover};
    border: 1px solid ${({ theme }) => theme.border};
    border-radius: 3px;
    padding: 1px 4px;
    font-size: 11px;
    font-family: inherit;
    color: ${({ theme }) => theme.textSecondary};
  }
`;
