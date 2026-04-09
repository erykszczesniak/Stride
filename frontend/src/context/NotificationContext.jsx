import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from "react";
import styled from "styled-components";
import { FiBell, FiX } from "react-icons/fi";
import notificationService from "../api/NotificationService";
import settingsService from "../api/SettingsService";
import { useAuth } from "./AuthContext";
import { useToast } from "./ToastContext";

const PromptOverlay = styled.div`
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 10000;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 16px;
  padding: 20px;
  max-width: 340px;
  box-shadow: 0 8px 32px ${({ theme }) => theme.shadow};
  animation: slideUp 0.3s ease;

  @keyframes slideUp {
    from { transform: translateY(20px); opacity: 0; }
    to { transform: translateY(0); opacity: 1; }
  }
`;

const PromptHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
`;

const PromptIcon = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(252, 76, 2, 0.1);
  color: #fc4c02;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
`;

const PromptTitle = styled.p`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
  flex: 1;
`;

const PromptClose = styled.button`
  background: none;
  border: none;
  padding: 4px;
  cursor: pointer;
  color: ${({ theme }) => theme.textMuted};
  border-radius: 4px;
  display: flex;
  align-items: center;

  &:hover { color: ${({ theme }) => theme.textPrimary}; }
`;

const PromptText = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 16px 0;
  line-height: 1.5;
`;

const PromptButtons = styled.div`
  display: flex;
  gap: 8px;
`;

const PromptBtn = styled.button`
  flex: 1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: ${({ $primary, theme }) => $primary ? "none" : `1px solid ${theme.border}`};
  background: ${({ $primary }) => $primary ? "#fc4c02" : "transparent"};
  color: ${({ $primary, theme }) => $primary ? "#fff" : theme.textSecondary};

  &:hover {
    background: ${({ $primary, theme }) => $primary ? "#e04400" : theme.bgHover};
  }
`;

const NotificationContext = createContext(null);

export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error("useNotifications must be used within a NotificationProvider");
  }
  return context;
};

const BASE_API_URL = process.env.REACT_APP_API_URL || "/api/v1";

export const NotificationProvider = ({ children }) => {
  const { user } = useAuth();
  const toast = useToast();
  const isAuthenticated = !!user;
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState([]);
  const [showPrompt, setShowPrompt] = useState(false);
  const pollRef = useRef(null);
  const sseRef = useRef(null);

  // Show notification permission prompt once per user
  useEffect(() => {
    if (!isAuthenticated || !user) return;
    const key = `notif_prompt_dismissed_${user.id || user.email}`;
    if (!localStorage.getItem(key)) {
      const timer = setTimeout(() => setShowPrompt(true), 2000);
      return () => clearTimeout(timer);
    }
  }, [isAuthenticated, user]);

  const handlePromptAllow = useCallback(() => {
    setShowPrompt(false);
    const key = `notif_prompt_dismissed_${user?.id || user?.email}`;
    localStorage.setItem(key, "allowed");
    settingsService.savePreferences({
      ...settingsService.getPreferences(),
      pushNotifications: true,
    });
  }, [user]);

  const handlePromptDismiss = useCallback(() => {
    setShowPrompt(false);
    const key = `notif_prompt_dismissed_${user?.id || user?.email}`;
    localStorage.setItem(key, "dismissed");
    settingsService.savePreferences({
      ...settingsService.getPreferences(),
      pushNotifications: false,
    });
  }, [user]);

  const fetchUnreadCount = useCallback(async () => {
    try {
      const response = await notificationService.getUnreadCount();
      setUnreadCount(response.data.count);
    } catch (error) {
      // Silently fail
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

  // Use ref for toast so SSE effect doesn't re-run when toast changes
  const toastRef = useRef(toast);
  useEffect(() => { toastRef.current = toast; }, [toast]);

  // SSE connection using native EventSource
  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem("token");
    if (!token) return;

    const url = `${BASE_API_URL}/notifications/stream?token=${encodeURIComponent(token)}`;
    const eventSource = new EventSource(url);

    eventSource.addEventListener("notification", (event) => {
      try {
        const notification = JSON.parse(event.data);
        setUnreadCount((prev) => prev + 1);
        setNotifications((prev) => [notification, ...prev]);
        const prefs = settingsService.getPreferences();
        if (prefs.pushNotifications !== false) {
          toastRef.current.notification(notification.message, "New Notification");
        }
      } catch (e) {
        // Invalid JSON
      }
    });

    eventSource.onerror = () => {
      // EventSource auto-reconnects, no manual handling needed
    };

    sseRef.current = eventSource;

    return () => {
      eventSource.close();
    };
  }, [isAuthenticated]);

  // Polling as fallback
  useEffect(() => {
    if (!isAuthenticated) return;
    fetchUnreadCount();
    pollRef.current = setInterval(fetchUnreadCount, 30000);
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [isAuthenticated, fetchUnreadCount]);

  const markAsRead = useCallback(async (id) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (error) {
      // Silently fail
    }
  }, []);

  const markAllAsRead = useCallback(async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (error) {
      // Silently fail
    }
  }, []);

  const deleteNotification = useCallback(async (id) => {
    try {
      const notification = notifications.find((n) => n.id === id);
      await notificationService.deleteNotification(id);
      setNotifications((prev) => prev.filter((n) => n.id !== id));
      if (notification && !notification.read) {
        setUnreadCount((prev) => Math.max(0, prev - 1));
      }
    } catch (error) {
      // Silently fail
    }
  }, [notifications]);

  const clearAllNotifications = useCallback(async () => {
    try {
      await notificationService.clearAll();
      setNotifications([]);
      setUnreadCount(0);
    } catch (error) {
      // Silently fail
    }
  }, []);

  return (
    <NotificationContext.Provider
      value={{
        unreadCount,
        notifications,
        fetchNotifications,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        clearAllNotifications,
      }}
    >
      {children}
      {showPrompt && (
        <PromptOverlay>
          <PromptHeader>
            <PromptIcon><FiBell size={18} /></PromptIcon>
            <PromptTitle>Enable notifications?</PromptTitle>
            <PromptClose onClick={handlePromptDismiss}><FiX size={16} /></PromptClose>
          </PromptHeader>
          <PromptText>
            Get real-time updates when someone likes your activity, comments, or starts following you.
          </PromptText>
          <PromptButtons>
            <PromptBtn onClick={handlePromptDismiss}>Not now</PromptBtn>
            <PromptBtn $primary onClick={handlePromptAllow}>Enable</PromptBtn>
          </PromptButtons>
        </PromptOverlay>
      )}
    </NotificationContext.Provider>
  );
};

export default NotificationContext;
