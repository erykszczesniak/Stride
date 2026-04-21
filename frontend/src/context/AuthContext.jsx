import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from "react";
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

  // Derive isAuthenticated from user instead of tracking separately
  const isAuthenticated = user !== null;

  const logout = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("refreshToken");
    setUser(null);
  }, []);

  const checkAuth = useCallback(async () => {
    const token = localStorage.getItem("token");
    if (token) {
      try {
        const response = await AuthService.getCurrentUser();
        setUser(response.data);
      } catch (error) {
        logout();
      }
    }
    setLoading(false);
  }, [logout]);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = useCallback(async (email, password) => {
    const response = await AuthService.login(email, password);
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("token", accessToken);
    localStorage.setItem("refreshToken", refreshToken);

    setUser(userData);

    return response.data;
  }, []);

  const googleLogin = useCallback(async (idToken) => {
    const response = await AuthService.googleLogin(idToken);
    const { accessToken, refreshToken, user: userData } = response.data;

    localStorage.setItem("token", accessToken);
    localStorage.setItem("refreshToken", refreshToken);

    setUser(userData);

    return response.data;
  }, []);

  /**
   * Finalize a sign-in when the tokens have already been issued by the backend and
   * arrived via a redirect (e.g. Strava OAuth2 callback). Stores both tokens, hydrates
   * the user via /users/me (the tokens don't include the full user payload), and lets
   * the caller decide where to navigate next.
   */
  const loginWithTokens = useCallback(async (accessToken, refreshToken) => {
    if (!accessToken) {
      throw new Error("Missing access token");
    }
    localStorage.setItem("token", accessToken);
    if (refreshToken) {
      localStorage.setItem("refreshToken", refreshToken);
    }
    const response = await AuthService.getCurrentUser();
    setUser(response.data);
    return response.data;
  }, []);

  const register = useCallback(async (firstName, lastName, email, password, confirmPassword) => {
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

    return response.data;
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    isAuthenticated,
    login,
    googleLogin,
    loginWithTokens,
    register,
    logout,
    checkAuth,
  }), [user, loading, isAuthenticated, login, googleLogin, loginWithTokens, register, logout, checkAuth]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
