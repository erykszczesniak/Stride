import styled from "styled-components";
import { Link } from "react-router-dom";

export const Card = styled(Link)`
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    background: #f1f5f9;
  }
`;

export const DateBox = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 10px;
  background: linear-gradient(135deg, #fc4c02 0%, #ff6b35 100%);
  border-radius: 12px;
`;

export const DateMonth = styled.span`
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

export const DateDay = styled.span`
  font-size: 22px;
  font-weight: 700;
  color: white;
  line-height: 1.2;
`;

export const EventInfo = styled.div`
  flex: 1;
  min-width: 0;
`;

export const EventName = styled.h4`
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
  line-height: 1.3;
`;

export const EventLocation = styled.p`
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;

  svg {
    color: #94a3b8;
    flex-shrink: 0;
  }
`;

export const EventMeta = styled.div`
  display: flex;
  gap: 12px;
`;

export const MetaItem = styled.span`
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;

  svg {
    width: 12px;
    height: 12px;
  }
`;

export const EventsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const EventsHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
`;

export const EventsTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
`;

export const ViewAllLink = styled(Link)`
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #fc4c02;
  text-decoration: none;
  font-weight: 500;
  transition: gap 0.2s;

  &:hover {
    gap: 8px;
  }

  &::after {
    content: "›";
    font-size: 16px;
  }
`;
