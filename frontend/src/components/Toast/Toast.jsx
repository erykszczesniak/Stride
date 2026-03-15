import React from "react";
import { FiCheck, FiX, FiAlertCircle, FiInfo } from "react-icons/fi";
import {
  ToastWrapper,
  IconWrapper,
  Content,
  Title,
  Message,
  CloseButton,
} from "./ToastStyles";

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
