import React, { createContext, useContext, useState, useEffect } from "react";
import AuthService from "../api/AuthService";

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = localStorage.getItem("token");
    if (token) {
      try {
        const response = await AuthService.getCurrentUser();
        setUser(response.data);
        setIsAuthenticated(true);
      } catch (error) {
        console.error("Auth check failed:", error);
        logout();
      }
    }
    setLoading(false);
  };

  const login = async (email, password) => {
    const response = await AuthService.login(email, password);
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("token", accessToken);
    localStorage.setItem("refreshToken", refreshToken);

    setUser(userData);
    setIsAuthenticated(true);

    return response.data;
  };

  const register = async (firstName, lastName, email, password, confirmPassword) => {
    const response = await AuthService.register(
      firstName,
      lastName,
      email,
      password,
      confirmPassword
    );
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("token", accessToken);
    localStorage.setItem("refreshToken", refreshToken);

    setUser(userData);
    setIsAuthenticated(true);

    return response.data;
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    setUser(null);
    setIsAuthenticated(false);
  };

  const value = {
    user,
    loading,
    isAuthenticated,
    login,
    register,
    logout,
    checkAuth,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
