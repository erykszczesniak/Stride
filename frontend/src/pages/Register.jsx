import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiMail, FiLock, FiActivity, FiEye, FiEyeOff, FiUser } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import styled from "styled-components";

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  background: #f8fafc;
`;

const LeftPanel = styled.div`
  flex: 1;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  color: white;

  @media (max-width: 968px) {
    display: none;
  }
`;

const BrandLogo = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
`;

const LogoIcon = styled.div`
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const LogoText = styled.span`
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
`;

const Tagline = styled.h2`
  font-size: 42px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 16px;
  line-height: 1.2;
`;

const Subtitle = styled.p`
  font-size: 18px;
  opacity: 0.9;
  text-align: center;
  max-width: 400px;
`;

const RightPanel = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  overflow-y: auto;
`;

const FormContainer = styled.div`
  width: 100%;
  max-width: 420px;
`;

const MobileLogo = styled.div`
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
  color: #fc4c02;

  @media (max-width: 968px) {
    display: flex;
  }
`;

const MobileLogoIcon = styled.div`
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
`;

const MobileLogoText = styled.span`
  font-size: 24px;
  font-weight: 800;
`;

const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
`;

const Description = styled.p`
  font-size: 15px;
  color: #64748b;
  margin-bottom: 32px;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 18px;
`;

const Row = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
`;

const InputGroup = styled.div`
  position: relative;
`;

const InputLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 8px;
`;

const InputWrapper = styled.div`
  position: relative;
  display: flex;
  align-items: center;
`;

const InputIcon = styled.div`
  position: absolute;
  left: 16px;
  color: #94a3b8;
`;

const Input = styled.input`
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid ${(props) => (props.error ? "#fecaca" : "#e2e8f0")};
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: ${(props) => (props.error ? "#dc2626" : "#fc4c02")};
    box-shadow: 0 0 0 3px
      ${(props) => (props.error ? "rgba(220, 38, 38, 0.1)" : "rgba(252, 76, 2, 0.1)")};
  }

  &::placeholder {
    color: #94a3b8;
  }
`;

const PasswordToggle = styled.button`
  position: absolute;
  right: 16px;
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;

  &:hover {
    color: #64748b;
  }
`;

const PasswordStrength = styled.div`
  display: flex;
  gap: 4px;
  margin-top: 8px;
`;

const StrengthBar = styled.div`
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: ${(props) => {
    if (!props.active) return "#e2e8f0";
    if (props.strength === 1) return "#ef4444";
    if (props.strength === 2) return "#f59e0b";
    if (props.strength === 3) return "#22c55e";
    return "#e2e8f0";
  }};
`;

const StrengthText = styled.span`
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
`;

const SubmitButton = styled.button`
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(252, 76, 2, 0.3);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }
`;

const Divider = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 24px 0;

  &::before,
  &::after {
    content: "";
    flex: 1;
    height: 1px;
    background: #e2e8f0;
  }

  span {
    font-size: 13px;
    color: #94a3b8;
  }
`;

const LoginLink = styled.p`
  text-align: center;
  font-size: 14px;
  color: #64748b;

  a {
    color: #fc4c02;
    text-decoration: none;
    font-weight: 600;

    &:hover {
      text-decoration: underline;
    }
  }
`;

const ErrorMessage = styled.div`
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  margin-bottom: 8px;
`;

const FieldError = styled.span`
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
  display: block;
`;

const Register = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
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
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});

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

          {error && <ErrorMessage>{error}</ErrorMessage>}

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

          <Divider>
            <span>or</span>
          </Divider>

          <LoginLink>
            Already have an account? <Link to="/login">Sign in</Link>
          </LoginLink>
        </FormContainer>
      </RightPanel>
    </Container>
  );
};

export default Register;
