import styled from "styled-components";
import { Link } from "react-router-dom";

export const Card = styled(Link)`
  display: block;
  background: ${({ theme }) => theme.bgCard};
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px ${({ theme }) => theme.shadow};
  }
`;

export const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

export const UserAvatar = styled.div`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const UserInfo = styled.div`
  flex: 1;
  min-width: 0;
`;

export const UserName = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 2px 0;
  transition: color 0.2s ease;

  &:hover {
    color: #fc5200;
  }
`;

export const ActivityTime = styled.p`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
  transition: color 0.3s ease;
`;

export const ActivityType = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${(props) => props.bgColor || props.theme.bgHover};
  color: ${(props) => props.color || props.theme.textSecondary};
  flex-shrink: 0;

  svg {
    width: 18px;
    height: 18px;
  }
`;

export const ActivityTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 4px 0;
  line-height: 1.4;
  transition: color 0.3s ease;
`;

export const ActivityDescription = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 16px 0;
  line-height: 1.5;
  transition: color 0.3s ease;
`;

export const StatsRow = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  margin-bottom: 16px;
  transition: background-color 0.3s ease;
`;

export const StatItem = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
`;

export const StatIcon = styled.div`
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textSecondary};
  transition: background-color 0.3s ease;

  svg {
    width: 14px;
    height: 14px;
  }
`;

export const StatContent = styled.div`
  display: flex;
  flex-direction: column;
`;

export const StatLabel = styled.span`
  font-size: 11px;
  color: ${({ theme }) => theme.textMuted};
  font-weight: 500;
  transition: color 0.3s ease;
`;

export const StatValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const CardFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid ${({ theme }) => theme.border};
  transition: border-color 0.3s ease;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: ${(props) => (props.active ? props.theme.primary : props.theme.textSecondary)};
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
    color: ${(props) => (props.active ? props.theme.primary : props.theme.textPrimary)};
  }

  svg {
    fill: ${(props) => (props.active ? props.theme.primary : "transparent")};
  }
`;

export const ActionsGroup = styled.div`
  display: flex;
  gap: 4px;
`;

export const CoverPhoto = styled.div`
  margin: 12px 0 16px 0;
  border-radius: 12px;
  overflow: hidden;
  height: 200px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const MapPreview = styled.div`
  margin: 12px 0 16px 0;
  border-radius: 12px;
  overflow: hidden;
  height: 160px;

  .leaflet-container {
    height: 100%;
    width: 100%;
    background: ${({ theme }) => theme.bgHover};
  }

  .leaflet-control-attribution {
    display: none;
  }

  .leaflet-control-zoom {
    display: none;
  }
`;
