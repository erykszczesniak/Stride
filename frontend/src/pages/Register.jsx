import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiMail, FiLock, FiActivity, FiEye, FiEyeOff, FiUser, FiSun, FiMoon } from "react-icons/fi";
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
  Row,
  InputGroup,
  InputLabel,
  InputWrapper,
  InputIcon,
  Input,
  PasswordToggle,
  PasswordStrength,
  StrengthBar,
  StrengthText,
  SubmitButton,
  Divider,
  LoginLink,
  ErrorMessage,
  FieldError,
  GoogleButton,
} from "./RegisterStyles";

const GOOGLE_CLIENT_ID = process.env.REACT_APP_GOOGLE_CLIENT_ID;

const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
    <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844a4.14 4.14 0 01-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z" fill="#34A853"/>
    <path d="M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.997 8.997 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
  </svg>
);

const Register = () => {
  const navigate = useNavigate();
  const { register, googleLogin } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const googleInitialized = useRef(false);

  const handleGoogleResponse = useCallback(async (response) => {
    setError("");
    setGoogleLoading(true);
    try {
      await googleLogin(response.credential);
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || "Google sign-up failed. Please try again."
      );
    } finally {
      setGoogleLoading(false);
    }
  }, [googleLogin, navigate]);

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

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => ({ ...prev, [name]: "" }));
  };

  const getPasswordStrength = (password) => {
    if (!password) return 0;
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password) && /[a-z]/.test(password)) strength++;
    if (/[0-9]/.test(password) || /[^A-Za-z0-9]/.test(password)) strength++;
    return strength;
  };

  const getStrengthText = (strength) => {
    if (strength === 0) return "";
    if (strength === 1) return "Weak";
    if (strength === 2) return "Medium";
    return "Strong";
  };

  const validate = () => {
    const errors = {};

    if (!formData.firstName.trim()) {
      errors.firstName = "First name is required";
    }
    if (!formData.lastName.trim()) {
      errors.lastName = "Last name is required";
    }
    if (!formData.email.trim()) {
      errors.email = "Email is required";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = "Invalid email format";
    }
    if (!formData.password) {
      errors.password = "Password is required";
    } else if (formData.password.length < 8) {
      errors.password = "Password must be at least 8 characters";
    }
    if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = "Passwords do not match";
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!validate()) return;

    setLoading(true);

    try {
      await register(
        formData.firstName,
        formData.lastName,
        formData.email,
        formData.password,
        formData.confirmPassword
      );
      navigate("/");
    } catch (err) {
      setError(
        err.response?.data?.message || "Registration failed. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const passwordStrength = getPasswordStrength(formData.password);

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
        <Tagline>Start Your Journey</Tagline>
        <Subtitle>
          Create your account and join the community of athletes pushing their limits every day.
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

          <Title>Create account</Title>
          <Description>Fill in your details to get started</Description>

          {error && <ErrorMessage role="alert">{error}</ErrorMessage>}

          <Form onSubmit={handleSubmit}>
            <Row>
              <InputGroup>
                <InputLabel>First Name</InputLabel>
                <InputWrapper>
                  <InputIcon>
                    <FiUser size={18} />
                  </InputIcon>
                  <Input
                    type="text"
                    name="firstName"
                    placeholder="John"
                    value={formData.firstName}
                    onChange={handleChange}
                    error={fieldErrors.firstName}
                  />
                </InputWrapper>
                {fieldErrors.firstName && <FieldError>{fieldErrors.firstName}</FieldError>}
              </InputGroup>

              <InputGroup>
                <InputLabel>Last Name</InputLabel>
                <InputWrapper>
                  <InputIcon>
                    <FiUser size={18} />
                  </InputIcon>
                  <Input
                    type="text"
                    name="lastName"
                    placeholder="Doe"
                    value={formData.lastName}
                    onChange={handleChange}
                    error={fieldErrors.lastName}
                  />
                </InputWrapper>
                {fieldErrors.lastName && <FieldError>{fieldErrors.lastName}</FieldError>}
              </InputGroup>
            </Row>

            <InputGroup>
              <InputLabel>Email</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiMail size={18} />
                </InputIcon>
                <Input
                  type="email"
                  name="email"
                  placeholder="john@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  error={fieldErrors.email}
                />
              </InputWrapper>
              {fieldErrors.email && <FieldError>{fieldErrors.email}</FieldError>}
            </InputGroup>

            <InputGroup>
              <InputLabel>Password</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiLock size={18} />
                </InputIcon>
                <Input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  placeholder="Min. 8 characters"
                  value={formData.password}
                  onChange={handleChange}
                  error={fieldErrors.password}
                />
                <PasswordToggle
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </PasswordToggle>
              </InputWrapper>
              {formData.password && (
                <>
                  <PasswordStrength>
                    <StrengthBar active={passwordStrength >= 1} strength={passwordStrength} />
                    <StrengthBar active={passwordStrength >= 2} strength={passwordStrength} />
                    <StrengthBar active={passwordStrength >= 3} strength={passwordStrength} />
                  </PasswordStrength>
                  <StrengthText>{getStrengthText(passwordStrength)}</StrengthText>
                </>
              )}
              {fieldErrors.password && <FieldError>{fieldErrors.password}</FieldError>}
            </InputGroup>

            <InputGroup>
              <InputLabel>Confirm Password</InputLabel>
              <InputWrapper>
                <InputIcon>
                  <FiLock size={18} />
                </InputIcon>
                <Input
                  type={showConfirmPassword ? "text" : "password"}
                  name="confirmPassword"
                  placeholder="Repeat your password"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  error={fieldErrors.confirmPassword}
                />
                <PasswordToggle
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={showConfirmPassword ? "Hide password confirmation" : "Show password confirmation"}
                >
                  {showConfirmPassword ? <FiEyeOff size={18} /> : <FiEye size={18} />}
                </PasswordToggle>
              </InputWrapper>
              {fieldErrors.confirmPassword && (
                <FieldError>{fieldErrors.confirmPassword}</FieldError>
              )}
            </InputGroup>

            <SubmitButton type="submit" disabled={loading}>
              {loading ? "Creating account..." : "Create Account"}
            </SubmitButton>
          </Form>

          {GOOGLE_CLIENT_ID && (
            <>
              <Divider>
                <span>or</span>
              </Divider>

              <GoogleButton
                type="button"
                onClick={handleGoogleClick}
                disabled={googleLoading}
              >
                <GoogleIcon />
                {googleLoading ? "Signing up..." : "Continue with Google"}
              </GoogleButton>
            </>
          )}

          {!GOOGLE_CLIENT_ID && (
            <Divider>
              <span>or</span>
            </Divider>
          )}

          <LoginLink>
            Already have an account? <Link to="/login">Sign in</Link>
          </LoginLink>
        </FormContainer>
      </RightPanel>
    </Container>
  );
};

export default Register;
