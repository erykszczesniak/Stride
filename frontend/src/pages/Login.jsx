import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FiMail, FiLock, FiActivity, FiEye, FiEyeOff, FiSun, FiMoon } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useToast } from "../context/ToastContext";
import AuthService from "../api/AuthService";
import {
  Container,
  LeftPanel,
  BrandLogo,
  LogoIcon,
  LogoText,
  Tagline,
  Subtitle,
  RightPanel,
  FormContainer,
  MobileLogo,
  MobileLogoIcon,
  MobileLogoText,
  Title,
  Description,
  Form,
  InputGroup,
  InputLabel,
  InputWrapper,
  InputIcon,
  Input,
  PasswordToggle,
  ForgotLink,
  SubmitButton,
  Divider,
  RegisterLink,
  ErrorMessage,
  GoogleButton,
  StravaButton,
} from "./LoginStyles";

const GOOGLE_CLIENT_ID = process.env.REACT_APP_GOOGLE_CLIENT_ID;

const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
    <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 01-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/>
    <path d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.997 8.997 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
  </svg>
);

const StravaIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <path d="M15.387 17.944l-2.089-4.116h-3.065L15.387 24l5.15-10.172h-3.066m-7.008-5.599l2.836 5.599h4.172L10.463 0l-7 13.828h4.169"/>
  </svg>
);

const Login = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const toast = useToast();
  const { login, googleLogin } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const [error, setError] = useState("");
  const googleInitialized = useRef(false);
  const stravaErrorShown = useRef(false);

  // Show a toast when the user was bounced back from a failed Strava OAuth flow.
  // useRef guard + setSearchParams cleanup keeps this effect firing exactly once.
  useEffect(() => {
    if (stravaErrorShown.current) return;
    const stravaError = searchParams.get("stravaError");
    if (!stravaError) return;
    stravaErrorShown.current = true;
    const message = stravaError === "denied"
      ? t("auth.errors.stravaDenied")
      : t("auth.errors.stravaFailed");
    toast.error(message);
    // Strip the query param so refreshes don't re-toast.
    const next = new URLSearchParams(searchParams);
    next.delete("stravaError");
    setSearchParams(next, { replace: true });
  }, [searchParams, setSearchParams, toast, t]);

  const handleStravaClick = () => {
    window.location.href = AuthService.stravaAuthorizeUrl();
  };

  const handleGoogleResponse = useCallback(async (response) => {
    setError("");
    setGoogleLoading(true);
    try {
      await googleLogin(response.credential);
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || t("auth.errors.googleLoginFailed")
      );
    } finally {
      setGoogleLoading(false);
    }
  }, [googleLogin, navigate, t]);

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID || googleInitialized.current) return;

    const initGoogle = () => {
      if (window.google?.accounts?.id) {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleGoogleResponse,
        });
        googleInitialized.current = true;
      }
    };

    initGoogle();

    if (!googleInitialized.current) {
      const interval = setInterval(() => {
        if (window.google?.accounts?.id) {
          initGoogle();
          clearInterval(interval);
        }
      }, 100);
      return () => clearInterval(interval);
    }
  }, [handleGoogleResponse]);

  const handleGoogleClick = () => {
    if (window.google?.accounts?.id) {
      window.google.accounts.id.prompt();
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login(email, password);
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || t("auth.errors.loginFailed")
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container>
      <button
        onClick={toggleTheme}
        title={isDarkMode ? t("nav.switchToLightMode") : t("nav.switchToDarkMode")}
        style={{
          position: "fixed",
          top: 20,
          right: 20,
          zIndex: 100,
          background: "none",
          border: "none",
          cursor: "pointer",
          color: isDarkMode ? "#ededef" : "#1a1a2e",
          opacity: 0.6,
          transition: "opacity 0.2s",
        }}
        onMouseEnter={(e) => (e.currentTarget.style.opacity = "1")}
        onMouseLeave={(e) => (e.currentTarget.style.opacity = "0.6")}
      >
        {isDarkMode ? <FiSun size={22} /> : <FiMoon size={22} />}
      </button>
      <LeftPanel>
        <BrandLogo>
          <LogoIcon>
            <FiActivity size={28} />
          </LogoIcon>
          <LogoText>STRIDE</LogoText>
        </BrandLogo>
        <Tagline>{t("auth.trackYourJourney")}</Tagline>
        <Subtitle>
          {t("auth.trackYourJourneySubtitle")}
        </Subtitle>
      </LeftPanel>

      <RightPanel>
        <FormContainer>
          <MobileLogo>
            <MobileLogoIcon>
              <FiActivity size={22} />
            </MobileLogoIcon>
            <MobileLogoText>STRIDE</MobileLogoText>
          </MobileLogo>

          <Title>{t("auth.welcomeBack")}</Title>
          <Description>{t("auth.welcomeDescription")}</Description>

          {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

          <Form onSubmit={handleSubmit}>
            <InputGroup>
              <InputLabel>{t("auth.email")}</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiMail size={18} />
                </InputIcon>
                <Input
                  type="email"
                  placeholder={t("auth.emailPlaceholder")}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </InputWrapper>
            </InputGroup>

            <InputGroup>
              <InputLabel>{t("auth.password")}</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiLock size={18} />
                </InputIcon>
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder={t("auth.passwordPlaceholder")}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <PasswordToggle
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}
                >
                  {showPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </PasswordToggle>
              </InputWrapper>
            </InputGroup>

            <ForgotLink to="/forgot-password">{t("auth.forgotPassword")}</ForgotLink>

            <SubmitButton type="submit" disabled={loading}>
              {loading ? t("auth.signingIn") : t("auth.signIn")}
            </SubmitButton>
          </Form>

          <Divider>
            <span>{t("auth.orDivider")}</span>
          </Divider>

          {GOOGLE_CLIENT_ID && (
            <GoogleButton
              type="button"
              onClick={handleGoogleClick}
              disabled={googleLoading}
            >
              <GoogleIcon />
              {googleLoading ? t("auth.signingIn") : t("auth.continueWithGoogle")}
            </GoogleButton>
          )}

          <StravaButton type="button" onClick={handleStravaClick}>
            <StravaIcon />
            {t("auth.continueWithStrava")}
          </StravaButton>

          <RegisterLink>
            {t("auth.dontHaveAccount")} <Link to="/register">{t("auth.createOne")}</Link>
          </RegisterLink>
        </FormContainer>
      </RightPanel>
    </Container>
  );
};

export default Login;
