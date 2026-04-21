import React, { useEffect, useRef } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import styled, { keyframes } from "styled-components";
import { FiActivity } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";

const spin = keyframes`
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
`;

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.bgPrimary};
  color: ${({ theme }) => theme.textPrimary};
  gap: 20px;
  padding: 20px;
`;

const Spinner = styled.div`
  width: 56px;
  height: 56px;
  border: 4px solid rgba(252, 76, 2, 0.2);
  border-top-color: #fc4c02;
  border-radius: 50%;
  animation: ${spin} 0.9s linear infinite;
`;

const Message = styled.p`
  font-size: 16px;
  color: ${({ theme }) => theme.textSecondary};
  text-align: center;
  max-width: 320px;
`;

const Brand = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fc4c02;
  font-weight: 700;
  font-size: 20px;
`;

/**
 * Landing page for the Strava OAuth2 login/signup redirect. The backend has already
 * exchanged the code for tokens and redirected here with them in the query string;
 * we just need to hand them to AuthContext and move on.
 */
const StravaCallback = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loginWithTokens } = useAuth();
  // Guard against React 18 StrictMode's double-mount re-running the effect.
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    const token = searchParams.get("token");
    const refreshToken = searchParams.get("refreshToken");
    const errorParam = searchParams.get("stravaError");

    if (errorParam) {
      navigate(`/login?stravaError=${encodeURIComponent(errorParam)}`, { replace: true });
      return;
    }

    if (!token) {
      navigate("/login?stravaError=callback-failed", { replace: true });
      return;
    }

    loginWithTokens(token, refreshToken)
      .then(() => navigate("/", { replace: true }))
      .catch(() => navigate("/login?stravaError=callback-failed", { replace: true }));
  }, [loginWithTokens, navigate, searchParams]);

  return (
    <Container>
      <Brand>
        <FiActivity size={22} />
        STRIDE
      </Brand>
      <Spinner />
      <Message>{t("auth.stravaSigningIn")}</Message>
    </Container>
  );
};

export default StravaCallback;
