import React, { createContext, useContext, useState, useCallback } from "react";
import styled from "styled-components";
import Toast from "../components/Toast/Toast";

const ToastContainer = styled.div`
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 400px;
`;

const ToastContext = createContext(null);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
};

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const removeToast = useCallback((id) => {
    setToasts((prev) =>
      prev.map((toast) =>
        toast.id === id ? { ...toast, isExiting: true } : toast
      )
    );
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 300);
  }, []);

  const addToast = useCallback(
    ({ type = "info", message, title, duration = 5000 }) => {
      const id = Date.now() + Math.random();
      const newToast = { id, type, message, title, isExiting: false };

      setToasts((prev) => [...prev, newToast]);

      if (duration > 0) {
        setTimeout(() => {
          removeToast(id);
        }, duration);
      }

      return id;
    },
    [removeToast]
  );

  const success = useCallback(
    (message, title) => addToast({ type: "success", message, title }),
    [addToast]
  );

  const error = useCallback(
    (message, title) => addToast({ type: "error", message, title }),
    [addToast]
  );

  const warning = useCallback(
    (message, title) => addToast({ type: "warning", message, title }),
    [addToast]
  );

  const info = useCallback(
    (message, title) => addToast({ type: "info", message, title }),
    [addToast]
  );

  const value = {
    addToast,
    removeToast,
    success,
    error,
    warning,
    info,
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastContainer>
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            type={toast.type}
            message={toast.message}
            title={toast.title}
            isExiting={toast.isExiting}
            onClose={() => removeToast(toast.id)}
          />
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
};

export default ToastContext;
