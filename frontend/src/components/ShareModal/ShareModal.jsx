import React, { useState } from "react";
import { FiX, FiCopy, FiCheck } from "react-icons/fi";
import {
  Overlay,
  Modal,
  Header,
  Title,
  CloseButton,
  UrlContainer,
  UrlInput,
  CopyButton,
  HelpText,
} from "./ShareModalStyles";

const ShareModal = ({ isOpen, onClose, activityId }) => {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;

  const shareUrl = `${window.location.origin}/activities/${activityId}`;

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
    }
  };

  const handleOverlayClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <Overlay onClick={handleOverlayClick}>
      <Modal>
        <Header>
          <Title>Share Activity</Title>
          <CloseButton onClick={onClose} aria-label="Close share dialog">
            <FiX size={20} />
          </CloseButton>
        </Header>
        <UrlContainer>
          <UrlInput type="text" value={shareUrl} readOnly />
          <CopyButton onClick={handleCopy} copied={copied}>
            {copied ? <FiCheck size={20} /> : <FiCopy size={20} />}
          </CopyButton>
        </UrlContainer>
        <HelpText>Share this link with friends to show them your activity</HelpText>
      </Modal>
    </Overlay>
  );
};

export default ShareModal;
