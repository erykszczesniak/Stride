import styled from "styled-components";

export const SettingsContainer = styled.div`
  display: flex;
  min-height: calc(100vh - 70px);
  background: ${({ theme }) => theme.bgPrimary};
`;

export const SettingsContent = styled.div`
  flex: 1;
  min-width: 0;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;

  @media (max-width: 768px) {
    padding: 16px;
  }
`;

export const SettingsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;

  @media (max-width: 600px) {
    flex-wrap: wrap;

    /* On narrow viewports drop the Save Changes button to a full-width row
       under the title so it doesn't get clipped or shove the title off-screen. */
    & > button {
      flex: 1 1 100%;
      justify-content: center;
    }
  }
`;

export const SettingsTitle = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
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
  background: ${({ theme }) => theme.bgCard};
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
  background: ${props => props.active ? props.theme.primaryLight : 'transparent'};
  color: ${props => props.active ? '#fc5200' : props.theme.textSecondary};
  font-size: 14px;
  font-weight: ${props => props.active ? '600' : '500'};
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  text-align: left;

  &:hover {
    background: ${props => props.active ? props.theme.primaryLight : props.theme.bgHover};
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
  /* Without min-width:0 a flex child defaults to min-width:auto, which is the
     content's intrinsic width. Wide form rows then refuse to shrink and push
     the whole layout past the viewport. */
  min-width: 0;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);

  @media (max-width: 768px) {
    padding: 20px;
  }
`;

export const PanelTitle = styled.h2`
  font-size: 20px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

export const PanelDescription = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 24px 0;
`;

export const ProfileHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid ${({ theme }) => theme.borderLight};
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
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 4px 0;
`;

export const ProfileEmail = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

export const FormGroup = styled.div`
  margin-bottom: 24px;
`;

export const FormLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

export const FormInput = styled.input`
  width: 100%;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};
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
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
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
  background: ${({ theme }) => theme.bgSecondary};
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
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 4px 0;
`;

export const ToggleDescription = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
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
    background-color: ${({ theme }) => theme.border};
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
    background-color: #fc4c02;
  }

  input:checked + span:before {
    transform: translateX(24px);
  }
`;

export const DarkModeToggle = styled(ToggleItem)`
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.borderLight};
  margin-top: 24px;
`;

export const DarkModeIcon = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const AccountEmail = styled.div`
  padding: 16px 20px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 12px;
  margin-bottom: 24px;
`;

export const AccountEmailLabel = styled.p`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 4px 0;
`;

export const AccountEmailValue = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const SignOutButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 24px;
  background: ${({ theme }) => theme.bgCard};
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

export const SectionDivider = styled.div`
  border-top: 1px solid ${({ theme }) => theme.borderLight};
  margin: 32px 0;
`;

export const SectionTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
`;

export const DangerZone = styled.div`
  border: 1px solid #ef4444;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
`;

export const DangerZoneTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #ef4444;
  margin: 0 0 8px 0;
`;

export const DangerZoneDescription = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 16px 0;
`;

export const DangerButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    background: #dc2626;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

export const ModalOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

export const ModalContent = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 32px;
  max-width: 440px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
`;

export const ModalTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 12px 0;
`;

export const ModalText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 24px 0;
  line-height: 1.5;
`;

export const ModalActions = styled.div`
  display: flex;
  gap: 12px;
  justify-content: flex-end;
`;

export const ModalCancelButton = styled.button`
  padding: 10px 20px;
  background: ${({ theme }) => theme.bgSecondary};
  color: ${({ theme }) => theme.textPrimary};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const PasswordFormButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
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

export const FormError = styled.p`
  font-size: 13px;
  color: #ef4444;
  margin: 4px 0 0 0;
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
    border: 3px solid ${({ theme }) => theme.border};
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

/* ------------------------- Strava integration styles -------------------------- */

export const IntegrationRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  background: ${({ theme }) => theme.cardBg || theme.bg};
  margin-bottom: 12px;
`;

export const IntegrationInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
`;

export const IntegrationName = styled.p`
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: ${({ theme }) => theme.text};
`;

export const IntegrationDetail = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary || theme.text};
  margin: 0;
`;

export const IntegrationActions = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
`;

export const StravaConnectButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fc5200;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover:not(:disabled) {
    background: #e04800;
  }

  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
`;

export const SecondaryButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: transparent;
  color: ${({ theme }) => theme.text};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.hover || "rgba(0,0,0,0.04)"};
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

/* ------------------------------ Gear styles ------------------------------ */

export const GearHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
`;

export const AddGearButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #fc5200;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover:not(:disabled) {
    background: #e54600;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;


export const GearSectionLabel = styled.h3`
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: ${({ theme }) => theme.textSecondary};
  margin: 24px 0 12px 0;
`;

export const GearList = styled.div`
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr;

  @media (min-width: 720px) {
    grid-template-columns: 1fr 1fr;
  }
`;

export const GearCard = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  background: ${({ theme }) => theme.surface};
  opacity: ${({ $retired }) => ($retired ? 0.6 : 1)};
`;

export const GearIcon = styled.div`
  font-size: 28px;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: ${({ theme }) => theme.backgroundElevated || theme.border};
`;

export const GearBody = styled.div`
  flex: 1;
  min-width: 0;
`;

export const GearName = styled.p`
  font-size: 15px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const GearMeta = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 8px 0;
`;

export const GearStats = styled.div`
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 10px;
`;

export const GearStatValue = styled.span`
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-right: 4px;
`;

export const GearActions = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

export const GearActionButton = styled.button`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  color: ${({ theme }) => theme.textSecondary};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(:disabled) {
    color: ${({ theme }) => theme.textPrimary};
    border-color: ${({ theme }) => theme.textSecondary};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const GearEmptyState = styled.div`
  text-align: center;
  padding: 48px 20px;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 14px;
`;

export const GearTypeChips = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
`;

export const GearTypeChip = styled.label`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid ${({ theme, $selected }) => ($selected ? "#fc5200" : theme.border)};
  border-radius: 6px;
  background: ${({ $selected }) => ($selected ? "rgba(252, 82, 0, 0.08)" : "transparent")};
  color: ${({ theme, $selected }) => ($selected ? "#fc5200" : theme.textSecondary)};
  font-size: 12px;
  cursor: pointer;
  user-select: none;

  input {
    display: none;
  }
`;
