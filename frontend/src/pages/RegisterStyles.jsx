import styled from "styled-components";

export const Container = styled.div`
  min-height: 100vh;
  display: flex;
  background: ${({ theme }) => theme.bgPrimary};
`;

export const LeftPanel = styled.div`
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

export const BrandLogo = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
`;

export const LogoIcon = styled.div`
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const LogoText = styled.span`
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
`;

export const Tagline = styled.h2`
  font-size: 42px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 16px;
  line-height: 1.2;
`;

export const Subtitle = styled.p`
  font-size: 18px;
  opacity: 0.9;
  text-align: center;
  max-width: 400px;
`;

export const RightPanel = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  overflow-y: auto;
`;

export const FormContainer = styled.div`
  width: 100%;
  max-width: 420px;
`;

export const MobileLogo = styled.div`
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
  color: #fc4c02;

  @media (max-width: 968px) {
    display: flex;
  }
`;

export const MobileLogoIcon = styled.div`
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
`;

export const MobileLogoText = styled.span`
  font-size: 24px;
  font-weight: 800;
`;

export const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

export const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 32px;
`;

export const Form = styled.form`
  display: flex;
  flex-direction: column;
  gap: 18px;
`;

export const Row = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
`;

export const InputGroup = styled.div`
  position: relative;
`;

export const InputLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

export const InputWrapper = styled.div`
  position: relative;
  display: flex;
  align-items: center;
`;

export const InputIcon = styled.div`
  position: absolute;
  left: 16px;
  color: #94a3b8;
`;

export const Input = styled.input`
  width: 100%;
  padding: 14px 16px 14px 48px;
  border: 1px solid ${(props) => (props.error ? "#fecaca" : props.theme.border)};
  border-radius: 12px;
  font-size: 15px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: ${(props) => (props.error ? "#dc2626" : "#fc4c02")};
    box-shadow: 0 0 0 3px
      ${(props) => (props.error ? "rgba(220, 38, 38, 0.1)" : "rgba(252, 76, 2, 0.1)")};
  }

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
  }
`;

export const PasswordToggle = styled.button`
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

export const PasswordStrength = styled.div`
  display: flex;
  gap: 4px;
  margin-top: 8px;
`;

export const StrengthBar = styled.div`
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: ${(props) => {
    if (!props.active) return props.theme.border;
    if (props.strength === 1) return "#ef4444";
    if (props.strength === 2) return "#f59e0b";
    if (props.strength === 3) return "#22c55e";
    return props.theme.border;
  }};
`;

export const StrengthText = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  margin-top: 4px;
`;

export const SubmitButton = styled.button`
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

export const Divider = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 12px 0;

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

export const LoginLink = styled.p`
  text-align: center;
  font-size: 14px;
  margin-top: 20px;
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

export const GoogleButton = styled.button`
  width: 100%;
  padding: 14px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;

  &:hover {
    background: ${({ theme }) => theme.bgSecondary || theme.bgInput};
    border-color: ${({ theme }) => theme.textMuted};
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }

  svg {
    flex-shrink: 0;
  }
`;

export const ErrorMessage = styled.div`
  background: rgba(220, 38, 38, 0.1);
  border: 1px solid rgba(220, 38, 38, 0.3);
  color: #ef4444;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 14px;
  margin-bottom: 8px;
`;

export const FieldError = styled.span`
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
  display: block;
`;
