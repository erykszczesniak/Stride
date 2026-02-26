import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { FiMail, FiLock, FiActivity, FiEye, FiEyeOff } from "react-icons/fi";
import { useAuth } from "../context/AuthContext";
import styled from "styled-components";

const Container = styled.div`
  min-height: 100vh;
  display: flex;
  background: ${({ theme }) => theme.bgPrimary};
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
`;

const FormContainer = styled.div`
  width: 100%;
  max-width: 420px;
`;

const MobileLogo = styled.div`
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 40px;
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
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 32px;
`;

const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 20px;
`;

const InputGroup = styled.div`
  position: relative;
`;

const InputLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
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
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  font-size: 15px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
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

const ForgotLink = styled(Link)`
  font-size: 14px;
  color: #fc4c02;
  text-decoration: none;
  text-align: right;
  margin-top: -12px;

  &:hover {
    text-decoration: underline;
  }
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
    background: ${({ theme }) => theme.border};
  }

  span {
    font-size: 13px;
    color: ${({ theme }) => theme.textMuted};
  }
`;

const RegisterLink = styled.p`
  text-align: center;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};

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
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #ef4444;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
`;

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
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

          {error && <ErrorMessage>{error}</ErrorMessage>}

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
