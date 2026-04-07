import React, { useState } from "react";
import { FiCheck, FiX, FiAlertCircle, FiInfo, FiCopy } from "react-icons/fi";
import {
  ToastWrapper,
  IconWrapper,
  Content,
  Title,
  Message,
  CloseButton,
  CopyButton,
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

const Toast = ({ type = "info", message, title, details, onClose, isExiting }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async (e) => {
    e.stopPropagation();
    const text = details || message;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {}
  };

  return (
    <ToastWrapper $type={type} $isExiting={isExiting}>
      <IconWrapper $type={type}>{getIcon(type)}</IconWrapper>
      <Content>
        <Title>{title || getTitle(type)}</Title>
        <Message>{message}</Message>
      </Content>
      {type === "error" && (
        <CopyButton onClick={handleCopy} title="Copy error to clipboard">
          {copied ? <FiCheck size={14} /> : <FiCopy size={14} />}
        </CopyButton>
      )}
      <CloseButton onClick={onClose}>
        <FiX size={18} />
      </CloseButton>
    </ToastWrapper>
  );
};

export default Toast;
