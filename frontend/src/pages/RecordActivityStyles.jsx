import styled from 'styled-components';

export const Container = styled.div`
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
  background: ${({ theme }) => theme.bgPrimary};
  min-height: 100vh;
`;

export const Header = styled.div`
  margin-bottom: 40px;
`;

export const Title = styled.h1`
  font-size: 32px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const Section = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const SectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 24px 0;
`;

export const ActivityTypeGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  margin-top: 16px;
`;

export const ActivityTypeButton = styled.button`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
  background: ${props => props.selected ? `${props.color}15` : props.theme.bgCard};
  border: 2px solid ${props => props.selected ? props.color : props.theme.border};
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 140px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: ${props => props.color};
  }

  &:active {
    transform: translateY(0);
  }
`;

export const ActivityIcon = styled.div`
  font-size: 48px;
  margin-bottom: 12px;
`;

export const ActivityLabel = styled.span`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
`;

export const FormGroup = styled.div`
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const Label = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
`;

export const Input = styled.input`
  width: 100%;
  padding: 14px 16px;
  font-size: 16px;
  border: 2px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  transition: all 0.2s ease;
  font-family: inherit;
  box-sizing: border-box;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};

  &:focus {
    outline: none;
    border-color: #fc5200;
  }

  &::placeholder {
    color: #b0b0b0;
  }
`;

export const TextArea = styled.textarea`
  width: 100%;
  padding: 14px 16px;
  font-size: 16px;
  border: 2px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  transition: all 0.2s ease;
  font-family: inherit;
  resize: vertical;
  box-sizing: border-box;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};

  &:focus {
    outline: none;
    border-color: #fc5200;
  }

  &::placeholder {
    color: #b0b0b0;
  }
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 24px;
`;

export const StatLabel = styled.label`
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 8px;
`;

export const StatInput = styled.input`
  width: 100%;
  padding: 14px 16px;
  font-size: 18px;
  font-weight: 500;
  border: 2px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  transition: all 0.2s ease;
  font-family: inherit;
  box-sizing: border-box;
  text-align: center;
  color: ${({ theme }) => theme.textPrimary};
  background: ${({ theme }) => theme.bgInput};

  &:focus {
    outline: none;
    border-color: #fc5200;
  }

  &::placeholder {
    color: #b0b0b0;
  }

  /* Remove spinner for number inputs */
  &::-webkit-outer-spin-button,
  &::-webkit-inner-spin-button {
    -webkit-appearance: none;
    margin: 0;
  }

  &[type=number] {
    -moz-appearance: textfield;
  }
`;

export const SaveButton = styled.button`
  width: 100%;
  padding: 18px;
  font-size: 18px;
  font-weight: 600;
  color: white;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-top: 32px;
  box-shadow: 0 4px 12px rgba(252, 82, 0, 0.3);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(252, 82, 0, 0.4);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
`;

export const ErrorMessage = styled.div`
  background: #fee;
  color: #c00;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
  font-size: 14px;
  border-left: 4px solid #c00;
`;

export const PhotoUploadArea = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const PhotoUploadButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  border: 2px dashed ${({ theme }) => theme.border};
  border-radius: 12px;
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textSecondary};
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: inherit;

  &:hover {
    border-color: #fc5200;
    color: #fc5200;
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const PhotoPreviewGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
`;

export const PhotoPreviewItem = styled.div`
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
`;

export const PhotoPreviewImage = styled.img`
  width: 100%;
  height: 100%;
  object-fit: cover;
`;

export const PhotoRemoveButton = styled.button`
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease;

  &:hover {
    background: rgba(220, 38, 38, 0.9);
  }
`;

export const PhotoCount = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textMuted};
  text-align: center;
`;
