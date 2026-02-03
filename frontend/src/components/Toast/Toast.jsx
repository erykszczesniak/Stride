import React from "react";
import styled, { keyframes } from "styled-components";
import { FiCheck, FiX, FiAlertCircle, FiInfo } from "react-icons/fi";

const slideIn = keyframes`
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
`;

const slideOut = keyframes`
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
`;

const ToastWrapper = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  border-left: 4px solid ${({ $type }) => {
    switch ($type) {
      case "success":
        return "#22c55e";
      case "error":
        return "#ef4444";
      case "warning":
        return "#f59e0b";
      default:
        return "#3b82f6";
    }
  }};
  min-width: 300px;
  max-width: 400px;
  animation: ${({ $isExiting }) => ($isExiting ? slideOut : slideIn)} 0.3s ease forwards;
`;

const IconWrapper = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: ${({ $type }) => {
    switch ($type) {
      case "success":
        return "rgba(34, 197, 94, 0.1)";
      case "error":
        return "rgba(239, 68, 68, 0.1)";
      case "warning":
        return "rgba(245, 158, 11, 0.1)";
      default:
        return "rgba(59, 130, 246, 0.1)";
    }
  }};
  color: ${({ $type }) => {
    switch ($type) {
      case "success":
        return "#22c55e";
      case "error":
        return "#ef4444";
      case "warning":
        return "#f59e0b";
      default:
        return "#3b82f6";
    }
  }};
`;

const Content = styled.div`
  flex: 1;
`;

const Title = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 2px 0;
`;

const Message = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
  line-height: 1.4;
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: ${({ theme }) => theme.textMuted};
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
    color: ${({ theme }) => theme.textPrimary};
  }
`;

const getIcon = (type) => {
  switch (type) {
    case "success":
      return <FiCheck size={18} />;
    case "error":
      return <FiX size={18} />;
    case "warning":
      return <FiAlertCircle size={18} />;
    default:
      return <FiInfo size={18} />;
  }
};

const getTitle = (type) => {
  switch (type) {
    case "success":
      return "Success";
    case "error":
      return "Error";
    case "warning":
      return "Warning";
    default:
      return "Info";
  }
};

const Toast = ({ type = "info", message, title, onClose, isExiting }) => {
  return (
    <ToastWrapper $type={type} $isExiting={isExiting}>
      <IconWrapper $type={type}>{getIcon(type)}</IconWrapper>
      <Content>
        <Title>{title || getTitle(type)}</Title>
        <Message>{message}</Message>
      </Content>
      <CloseButton onClick={onClose}>
        <FiX size={18} />
      </CloseButton>
    </ToastWrapper>
  );
};

export default Toast;
