import styled from "styled-components";
import { Link } from "react-router-dom";

export const Card = styled(Link)`
  display: block;
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

export const CardHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
`;

export const UserAvatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #fc4c02;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
`;

export const UserInfo = styled.div`
  flex: 1;
`;

export const UserName = styled.p`
  font-size: 14px;
  font-weight: 600;
  color: #333;
`;

export const ActivityTime = styled.p`
  font-size: 12px;
  color: #666;
`;

export const ActivityType = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background-color: ${(props) => props.bgColor || "#f0f0f0"};
  color: ${(props) => props.color || "#666"};
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
`;

export const ActivityTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
`;

export const StatsRow = styled.div`
  display: flex;
  gap: 24px;
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
`;

export const StatItem = styled.div`
  display: flex;
  flex-direction: column;
`;

export const StatLabel = styled.span`
  font-size: 11px;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

export const StatValue = styled.span`
  font-size: 15px;
  font-weight: 600;
  color: #333;
`;

export const CardFooter = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: ${(props) => (props.active ? "#fc4c02" : "#666")};
  font-size: 13px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 4px;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f5f5;
  }
`;

export const ActionsGroup = styled.div`
  display: flex;
  gap: 8px;
`;
