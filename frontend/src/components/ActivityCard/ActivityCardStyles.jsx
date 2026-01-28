import styled from "styled-components";
import { Link } from "react-router-dom";

export const Card = styled(Link)`
  display: block;
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.06);
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
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
  color: #1a1a2e;
  margin: 0 0 2px 0;
`;

export const ActivityTime = styled.p`
  font-size: 12px;
  color: #64748b;
  margin: 0;
`;

export const ActivityType = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: ${(props) => props.bgColor || "#f0f0f0"};
  color: ${(props) => props.color || "#666"};
  flex-shrink: 0;

  svg {
    width: 18px;
    height: 18px;
  }
`;

export const ActivityTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px 0;
  line-height: 1.4;
`;

export const ActivityDescription = styled.p`
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px 0;
  line-height: 1.5;
`;

export const StatsRow = styled.div`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 16px;
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
  background: white;
  color: #64748b;

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
  color: #94a3b8;
  font-weight: 500;
`;

export const StatValue = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
`;

export const CardFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: ${(props) => (props.active ? "#fc4c02" : "#64748b")};
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.2s ease;

  &:hover {
    background-color: #f8fafc;
    color: ${(props) => (props.active ? "#fc4c02" : "#1a1a2e")};
  }

  svg {
    fill: ${(props) => (props.active ? "#fc4c02" : "transparent")};
  }
`;

export const ActionsGroup = styled.div`
  display: flex;
  gap: 4px;
`;
