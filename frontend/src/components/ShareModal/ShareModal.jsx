import React, { useState } from "react";
import { FiX, FiCopy, FiCheck } from "react-icons/fi";
import styled from "styled-components";

const Overlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const Modal = styled.div`
  background: white;
  border-radius: 12px;
  padding: 20px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  position: relative;
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
`;

const Title = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
`;

const CloseButton = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;

  &:hover {
    background-color: #f5f5f5;
  }
`;

const UrlContainer = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
`;

const UrlInput = styled.input`
  flex: 1;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #333;
  outline: none;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const CopyButton = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  color: ${(props) => (props.copied ? "#4caf50" : "#666")};
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background-color: #e0e0e0;
  }
`;

const HelpText = styled.p`
  font-size: 13px;
  color: #666;
  margin: 0;
`;

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
      console.error("Failed to copy:", err);
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
          <CloseButton onClick={onClose}>
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
