import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiBell, FiTrash2 } from "react-icons/fi";
import { useNotifications } from "../../context/NotificationContext";
import { getTimeAgo } from "../../utils/formatters";
import {
  NotificationWrapper,
  BellButton,
  BadgeCount,
  DropdownPanel,
  DropdownHeader,
  DropdownTitle,
  HeaderActions,
  MarkAllButton,
  NotificationList,
  NotificationItem,
  SenderAvatar,
  NotificationContent,
  NotificationMessage,
  NotificationTime,
  UnreadDot,
  DeleteButton,
  EmptyState,
  EmptyIcon,
  EmptyText,
} from "./NotificationDropdownStyles";

const TYPE_COLORS = {
  LIKE: "#ef4444",
  COMMENT: "#3b82f6",
  FOLLOW: "#8b5cf6",
  ACHIEVEMENT: "#f59e0b",
  GOAL_COMPLETED: "#10b981",
  SYSTEM: "#6b7280",
};

const TYPE_ICONS = {
  LIKE: "\u2764\uFE0F",
  COMMENT: "\uD83D\uDCAC",
  FOLLOW: "\uD83D\uDC64",
  ACHIEVEMENT: "\uD83C\uDFC6",
  GOAL_COMPLETED: "\uD83C\uDFAF",
  SYSTEM: "\uD83D\uDD14",
};

const NotificationDropdown = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { unreadCount, notifications, fetchNotifications, markAsRead, markAllAsRead, deleteNotification, clearAllNotifications } = useNotifications();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef(null);

  // Fetch full notifications when dropdown opens
  useEffect(() => {
    if (isOpen) {
      fetchNotifications();
    }
  }, [isOpen, fetchNotifications]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  const handleToggle = () => {
    setIsOpen((prev) => !prev);
  };

  const handleNotificationClick = async (notification) => {
    if (!notification.read) {
      markAsRead(notification.id);
    }

    setIsOpen(false);

    if (notification.referenceType === "ACTIVITY" && notification.referenceId) {
      navigate(`/activities/${notification.referenceId}`);
    } else if (notification.referenceType === "GOAL" && notification.referenceId) {
      navigate("/goals");
    } else if (notification.type === "FOLLOW") {
      navigate("/profile");
    }
  };

  const getInitials = (firstName, lastName) => {
    const first = (firstName || "")[0] || "";
    const last = (lastName || "")[0] || "";
    return (first + last).toUpperCase() || "?";
  };

  return (
    <NotificationWrapper ref={dropdownRef}>
      <BellButton title={t("notifications.title")} onClick={handleToggle}>
        <FiBell size={28} />
        {unreadCount > 0 && (
          <BadgeCount>{unreadCount > 99 ? "99+" : unreadCount}</BadgeCount>
        )}
      </BellButton>

      {isOpen && (
        <DropdownPanel>
          <DropdownHeader>
            <DropdownTitle>{t("notifications.title")}</DropdownTitle>
            <HeaderActions>
              {unreadCount > 0 && (
                <MarkAllButton onClick={markAllAsRead}>
                  {t("notifications.markAllAsRead")}
                </MarkAllButton>
              )}
              {notifications.length > 0 && (
                <MarkAllButton onClick={clearAllNotifications} title={t("notifications.clearAll")}>
                  <FiTrash2 size={14} />
                </MarkAllButton>
              )}
            </HeaderActions>
          </DropdownHeader>

          <NotificationList>
            {notifications.length === 0 ? (
              <EmptyState>
                <EmptyIcon>
                  <FiBell size={32} />
                </EmptyIcon>
                <EmptyText>{t("notifications.empty")}</EmptyText>
              </EmptyState>
            ) : (
              notifications.map((notification) => (
                <NotificationItem
                  key={notification.id}
                  $unread={!notification.read}
                  onClick={() => handleNotificationClick(notification)}
                >
                  <SenderAvatar $color={TYPE_COLORS[notification.type]}>
                    {notification.senderFirstName
                      ? getInitials(notification.senderFirstName, notification.senderLastName)
                      : TYPE_ICONS[notification.type] || "?"}
                  </SenderAvatar>
                  <NotificationContent>
                    <NotificationMessage $unread={!notification.read}>
                      {notification.message}
                    </NotificationMessage>
                    <NotificationTime>
                      {getTimeAgo(notification.createdAt)}
                    </NotificationTime>
                  </NotificationContent>
                  {!notification.read && <UnreadDot />}
                  <DeleteButton
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteNotification(notification.id);
                    }}
                    title={t("notifications.deleteNotification")}
                  >
                    <FiTrash2 size={14} />
                  </DeleteButton>
                </NotificationItem>
              ))
            )}
          </NotificationList>
        </DropdownPanel>
      )}
    </NotificationWrapper>
  );
};

export default NotificationDropdown;
