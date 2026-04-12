import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { FiCheck, FiX, FiAlertCircle, FiInfo, FiBell, FiCopy } from "react-icons/fi";
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
    case "notification":
      return <FiBell size={18} />;
    default:
      return <FiInfo size={18} />;
  }
};

const Toast = ({ type = "info", message, title, details, onClose, isExiting }) => {
  const { t } = useTranslation();
  const [copied, setCopied] = useState(false);

  const getTitle = (toastType) => {
    switch (toastType) {
      case "success":
        return t("common.success");
      case "error":
        return t("common.error");
      case "warning":
        return t("common.warning");
      default:
        return t("common.info");
    }
  };

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
        <CopyButton onClick={handleCopy} title={t("common.copy")}>
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
