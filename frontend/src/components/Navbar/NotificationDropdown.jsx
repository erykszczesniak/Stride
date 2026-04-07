import React, { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { FiBell } from "react-icons/fi";
import notificationService from "../../api/NotificationService";
import { getTimeAgo } from "../../utils/formatters";
import {
  NotificationWrapper,
  BellButton,
  BadgeCount,
  DropdownPanel,
  DropdownHeader,
  DropdownTitle,
  MarkAllButton,
  NotificationList,
  NotificationItem,
  SenderAvatar,
  NotificationContent,
  NotificationMessage,
  NotificationTime,
  UnreadDot,
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
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const dropdownRef = useRef(null);
  const pollRef = useRef(null);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const response = await notificationService.getUnreadCount();
      setUnreadCount(response.data.count);
    } catch (error) {
      // Silently fail for polling
    }
  }, []);

  const fetchNotifications = useCallback(async () => {
    try {
      const response = await notificationService.getNotifications();
      setNotifications(response.data);
    } catch (error) {
      // Silently fail
    }
  }, []);

  // Poll for unread count every 30 seconds
  useEffect(() => {
    fetchUnreadCount();
    pollRef.current = setInterval(fetchUnreadCount, 30000);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [fetchUnreadCount]);

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
      try {
        await notificationService.markAsRead(notification.id);
        setNotifications((prev) =>
          prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch (error) {
        // Silently fail
      }
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

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      // Silently fail
    }
  };

  const getInitials = (firstName, lastName) => {
    const first = (firstName || "")[0] || "";
    const last = (lastName || "")[0] || "";
    return (first + last).toUpperCase() || "?";
  };

  return (
    <NotificationWrapper ref={dropdownRef}>
      <BellButton title="Notifications" onClick={handleToggle}>
        <FiBell size={20} />
        {unreadCount > 0 && (
          <BadgeCount>{unreadCount > 99 ? "99+" : unreadCount}</BadgeCount>
        )}
      </BellButton>

      {isOpen && (
        <DropdownPanel>
          <DropdownHeader>
            <DropdownTitle>Notifications</DropdownTitle>
            {unreadCount > 0 && (
              <MarkAllButton onClick={handleMarkAllAsRead}>
                Mark all as read
              </MarkAllButton>
            )}
          </DropdownHeader>

          <NotificationList>
            {notifications.length === 0 ? (
              <EmptyState>
                <EmptyIcon>
                  <FiBell size={32} />
                </EmptyIcon>
                <EmptyText>No notifications yet</EmptyText>
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
