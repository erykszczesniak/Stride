import styled from "styled-components";
import { Link } from "react-router-dom";

export const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;

  &:hover {
    color: #fc4c02;
  }
`;

export const EventHeader = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const CoverImage = styled.div`
  height: 200px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const EventIcon = styled.span`
  font-size: 64px;
`;

export const HeaderContent = styled.div`
  padding: 24px;
`;

export const EventMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
`;

export const DateBadge = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  background: ${({ theme }) => theme.primaryLight};
  padding: 12px 16px;
  border-radius: 12px;
  min-width: 70px;
`;

export const DateMonth = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: #fc4c02;
  text-transform: uppercase;
`;

export const DateDay = styled.span`
  font-size: 28px;
  font-weight: 700;
  color: #fc4c02;
`;

export const DateYear = styled.span`
  font-size: 11px;
  color: #999;
`;

export const EventTitleSection = styled.div`
  flex: 1;
`;

export const EventType = styled.span`
  display: inline-block;
  background: ${props => props.color || '#fc4c02'};
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
`;

export const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 8px 0;
`;

export const Description = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin: 16px 0;
`;

export const ActionButtons = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 20px;
`;

export const RegisterButton = styled.button`
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;

  ${props => props.registered ? `
    background: #e8f5e9;
    color: #2e7d32;

    &:hover {
      background: #ffebee;
      color: #c62828;
    }
  ` : `
    background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
    color: white;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(252, 76, 2, 0.3);
    }
  `}

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
`;

export const SecondaryButton = styled.button`
  padding: 14px 20px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  border-radius: 8px;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }
`;

export const InfoCard = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

export const CardTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
`;

export const InfoItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
`;

export const InfoIcon = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: ${({ theme }) => theme.bgSecondary};
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.textSecondary};
  flex-shrink: 0;
`;

export const InfoContent = styled.div``;

export const InfoLabel = styled.p`
  font-size: 12px;
  color: #999;
  margin: 0 0 4px 0;
`;

export const InfoValue = styled.p`
  font-size: 15px;
  font-weight: 500;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const ParticipantsSection = styled.div`
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid ${({ theme }) => theme.borderLight};
`;

export const ProgressBar = styled.div`
  height: 8px;
  background: ${({ theme }) => theme.bgSecondary};
  border-radius: 4px;
  overflow: hidden;
  margin: 12px 0;
`;

export const ProgressFill = styled.div`
  height: 100%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  width: ${props => props.percent}%;
  border-radius: 4px;
  transition: width 0.3s ease;
`;

export const ParticipantStats = styled.div`
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const ExternalLink = styled.a`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #fc4c02;
  text-decoration: none;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
`;
