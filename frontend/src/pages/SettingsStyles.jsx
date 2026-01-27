import styled from "styled-components";

export const SettingsContainer = styled.div`
  display: flex;
  min-height: calc(100vh - 70px);
  background: #f5f5f5;
`;

export const SettingsContent = styled.div`
  flex: 1;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
`;

export const SettingsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
`;

export const SettingsTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
`;

export const SaveButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: #fc5200;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: #e04800;
  }

  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
`;

export const SettingsLayout = styled.div`
  display: flex;
  gap: 24px;

  @media (max-width: 768px) {
    flex-direction: column;
  }
`;

export const SettingsSidebar = styled.div`
  width: 240px;
  background: white;
  border-radius: 12px;
  padding: 8px;
  height: fit-content;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);

  @media (max-width: 768px) {
    width: 100%;
    display: flex;
    overflow-x: auto;
    gap: 4px;
  }
`;

export const SidebarItem = styled.button`
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  border: none;
  background: ${props => props.active ? '#fff7ed' : 'transparent'};
  color: ${props => props.active ? '#fc5200' : '#666'};
  font-size: 14px;
  font-weight: ${props => props.active ? '600' : '500'};
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  text-align: left;

  &:hover {
    background: ${props => props.active ? '#fff7ed' : '#f5f5f5'};
  }

  svg {
    flex-shrink: 0;
  }

  @media (max-width: 768px) {
    width: auto;
    white-space: nowrap;
  }
`;

export const SidebarArrow = styled.span`
  margin-left: auto;
  color: ${props => props.active ? '#fc5200' : 'transparent'};
`;

export const SettingsPanel = styled.div`
  flex: 1;
  background: white;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const PanelTitle = styled.h2`
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
`;

export const PanelDescription = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 24px 0;
`;

export const ProfileHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
`;

export const ProfileAvatar = styled.div`
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: white;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }
`;

export const ProfileInfo = styled.div``;

export const ProfileName = styled.p`
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
`;

export const ProfileEmail = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0;
`;

export const FormGroup = styled.div`
  margin-bottom: 24px;
`;

export const FormLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
`;

export const FormInput = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: #fc5200;
    box-shadow: 0 0 0 3px rgba(252, 82, 0, 0.1);
  }

  &::placeholder {
    color: #999;
  }
`;

export const FormRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

export const FormSelect = styled.select`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: #fc5200;
    box-shadow: 0 0 0 3px rgba(252, 82, 0, 0.1);
  }
`;

export const ToggleItem = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 12px;
  margin-bottom: 12px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const ToggleInfo = styled.div``;

export const ToggleTitle = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
`;

export const ToggleDescription = styled.p`
  font-size: 13px;
  color: #666;
  margin: 0;
`;

export const Toggle = styled.label`
  position: relative;
  display: inline-block;
  width: 52px;
  height: 28px;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  span {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #ccc;
    transition: 0.3s;
    border-radius: 28px;

    &:before {
      position: absolute;
      content: "";
      height: 22px;
      width: 22px;
      left: 3px;
      bottom: 3px;
      background-color: white;
      transition: 0.3s;
      border-radius: 50%;
    }
  }

  input:checked + span {
    background-color: #1a1a1a;
  }

  input:checked + span:before {
    transform: translateX(24px);
  }
`;

export const DarkModeToggle = styled(ToggleItem)`
  background: white;
  border: 1px solid #f0f0f0;
  margin-top: 24px;
`;

export const DarkModeIcon = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const AccountEmail = styled.div`
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 12px;
  margin-bottom: 24px;
`;

export const AccountEmailLabel = styled.p`
  font-size: 12px;
  color: #666;
  margin: 0 0 4px 0;
`;

export const AccountEmailValue = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
`;

export const SignOutButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 24px;
  background: white;
  border: 1px solid #fc5200;
  color: #fc5200;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #fff7ed;
  }
`;

export const LoadingSpinner = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 60px;

  &::after {
    content: "";
    width: 40px;
    height: 40px;
    border: 3px solid #f0f0f0;
    border-top-color: #fc5200;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
`;
