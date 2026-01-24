import styled from "styled-components";
import { Link } from "react-router-dom";

export const Card = styled(Link)`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  text-decoration: none;
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
`;

export const DateBox = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 8px;
  background-color: #fff3e0;
  border-radius: 8px;
`;

export const DateMonth = styled.span`
  font-size: 11px;
  font-weight: 600;
  color: #fc4c02;
  text-transform: uppercase;
`;

export const DateDay = styled.span`
  font-size: 24px;
  font-weight: 700;
  color: #fc4c02;
  line-height: 1;
`;

export const EventInfo = styled.div`
  flex: 1;
`;

export const EventName = styled.h4`
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
`;

export const EventLocation = styled.p`
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
`;

export const EventMeta = styled.div`
  display: flex;
  gap: 16px;
`;

export const MetaItem = styled.span`
  font-size: 12px;
  color: #888;
  display: flex;
  align-items: center;
  gap: 4px;
`;

export const EventsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const EventsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
`;

export const EventsTitle = styled.h3`
  font-size: 16px;
  font-weight: 600;
  color: #333;
`;

export const ViewAllLink = styled(Link)`
  font-size: 13px;
  color: #fc4c02;
  text-decoration: none;
  font-weight: 500;

  &:hover {
    text-decoration: underline;
  }
`;
