import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiMail, FiLock, FiActivity, FiEye, FiEyeOff, FiSun, FiMoon } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
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
} from "./LoginStyles";

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login(email, password);
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || "Invalid email or password. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container>
      <button
        onClick={toggleTheme}
        title={isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode"}
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
        <Tagline>Track Your Journey</Tagline>
        <Subtitle>
          Join thousands of athletes tracking their progress, setting goals, and achieving greatness.
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

          <Title>Welcome back</Title>
          <Description>Enter your credentials to access your account</Description>

          {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

          <Form onSubmit={handleSubmit}>
            <InputGroup>
              <InputLabel>Email</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiMail size={18} />
                </InputIcon>
                <Input
                  type="email"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </InputWrapper>
            </InputGroup>

            <InputGroup>
              <InputLabel>Password</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiLock size={18} />
                </InputIcon>
                <Input
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <PasswordToggle
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </PasswordToggle>
              </InputWrapper>
            </InputGroup>

            <ForgotLink to="/forgot-password">Forgot password?</ForgotLink>

            <SubmitButton type="submit" disabled={loading}>
              {loading ? "Signing in..." : "Sign In"}
            </SubmitButton>
          </Form>

          <Divider>
            <span>or</span>
          </Divider>

          <RegisterLink>
            Don't have an account? <Link to="/register">Create one</Link>
          </RegisterLink>
        </FormContainer>
      </RightPanel>
    </Container>
  );
};

export default Login;
