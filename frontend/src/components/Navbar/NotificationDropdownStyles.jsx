import styled, { keyframes } from "styled-components";

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
`;

export const NotificationWrapper = styled.div`
  position: relative;
`;

export const BellButton = styled.button`
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  min-width: 40px;
  min-height: 40px;
  position: relative;

  &:hover {
    color: white;
    background-color: rgba(255, 255, 255, 0.15);
  }
`;

export const BadgeCount = styled.span`
  position: absolute;
  top: 4px;
  right: 4px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
  border: 2px solid #fc4c02;
`;

export const DropdownPanel = styled.div`
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 380px;
  max-height: 480px;
  background: ${({ theme }) => theme.dropdownBg};
  border-radius: 12px;
  box-shadow: 0 8px 24px ${({ theme }) => theme.shadowMedium};
  border: 1px solid ${({ theme }) => theme.border};
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: ${fadeIn} 0.15s ease-out;
  z-index: 1001;
`;

export const DropdownHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
`;

export const DropdownTitle = styled.h3`
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const MarkAllButton = styled.button`
  background: none;
  border: none;
  color: ${({ theme }) => theme.primary};
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
  }
`;

export const NotificationList = styled.div`
  overflow-y: auto;
  flex: 1;
`;

export const NotificationItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
  background-color: ${({ $unread, theme }) => ($unread ? theme.bgHover : "transparent")};

  &:hover {
    background-color: ${({ theme }) => theme.dropdownHover};
  }

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.border};
  }
`;

export const SenderAvatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: ${({ $color }) => $color || "#fc4c02"};
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
`;

export const NotificationContent = styled.div`
  flex: 1;
  min-width: 0;
`;

export const NotificationMessage = styled.p`
  margin: 0;
  font-size: 13px;
  line-height: 1.4;
  color: ${({ theme }) => theme.textPrimary};
  font-weight: ${({ $unread }) => ($unread ? "500" : "400")};
`;

export const NotificationTime = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textMuted};
  margin-top: 2px;
  display: block;
`;

export const UnreadDot = styled.div`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: ${({ theme }) => theme.primary};
  flex-shrink: 0;
  margin-top: 6px;
`;

export const EmptyState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  color: ${({ theme }) => theme.textMuted};
`;

export const EmptyIcon = styled.div`
  font-size: 32px;
  margin-bottom: 8px;
  opacity: 0.5;
`;

export const EmptyText = styled.p`
  margin: 0;
  font-size: 14px;
`;
