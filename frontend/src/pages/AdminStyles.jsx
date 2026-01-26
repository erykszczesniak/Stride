import styled from "styled-components";
import { Link } from "react-router-dom";

export const Container = styled.div`
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
`;

export const Header = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
`;

export const BackButton = styled(Link)`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: white;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  color: #333;
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: #fc5200;
    color: #fc5200;
  }
`;

export const HeaderContent = styled.div`
  flex: 1;
`;

export const Title = styled.h1`
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 4px 0;
`;

export const Subtitle = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0;
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;

  @media (max-width: 1024px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

export const StatCard = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 1px solid #e5e5e5;
  transition: all 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
`;

export const StatIcon = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: ${props => props.color || '#fc5200'}20;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  color: ${props => props.color || '#fc5200'};
`;

export const StatValue = styled.div`
  font-size: 32px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 4px;
`;

export const StatLabel = styled.div`
  font-size: 14px;
  color: #666;
  font-weight: 500;
`;

export const TabsContainer = styled.div`
  display: flex;
  gap: 8px;
  background: white;
  border: 1px solid #e5e5e5;
  padding: 4px;
  border-radius: 12px;
  margin-bottom: 24px;
  width: fit-content;
`;

export const Tab = styled.button`
  padding: 12px 24px;
  background: ${props => props.active ? '#1a1a1a' : 'transparent'};
  color: ${props => props.active ? 'white' : '#666'};
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: ${props => props.active ? '#1a1a1a' : '#f5f5f5'};
  }
`;

export const TabContent = styled.div`
  background: white;
  border-radius: 16px;
  border: 1px solid #e5e5e5;
  overflow: hidden;
`;

export const TableHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e5e5;
`;

export const SearchInput = styled.input`
  padding: 10px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  width: 300px;
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

export const AddButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #fc5200;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: #e04800;
  }
`;

export const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
`;

export const Th = styled.th`
  text-align: left;
  padding: 16px 24px;
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #e5e5e5;
  background: #fafafa;
`;

export const Td = styled.td`
  padding: 16px 24px;
  font-size: 14px;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
`;

export const Tr = styled.tr`
  transition: background 0.2s ease;

  &:hover {
    background: #fafafa;
  }

  &:last-child td {
    border-bottom: none;
  }
`;

export const UserCell = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const UserAvatar = styled.div`
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: white;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }
`;

export const UserInfo = styled.div`
  display: flex;
  flex-direction: column;
`;

export const UserName = styled.span`
  font-weight: 600;
  color: #1a1a1a;
`;

export const UserEmail = styled.span`
  font-size: 12px;
  color: #666;
`;

export const Badge = styled.span`
  display: inline-block;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: ${props => {
    if (props.type === 'admin') return '#fee2e2';
    if (props.type === 'active') return '#dcfce7';
    if (props.type === 'inactive') return '#f3f4f6';
    return '#fef3c7';
  }};
  color: ${props => {
    if (props.type === 'admin') return '#dc2626';
    if (props.type === 'active') return '#16a34a';
    if (props.type === 'inactive') return '#6b7280';
    return '#d97706';
  }};
`;

export const ActionButton = styled.button`
  padding: 6px 12px;
  border: 1px solid #e5e5e5;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-right: 8px;

  &:hover {
    border-color: #fc5200;
    color: #fc5200;
  }

  &.danger:hover {
    border-color: #dc2626;
    color: #dc2626;
  }
`;

export const Pagination = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border-top: 1px solid #e5e5e5;
`;

export const PageButton = styled.button`
  padding: 8px 16px;
  border: 1px solid ${props => props.active ? '#fc5200' : '#e5e5e5'};
  background: ${props => props.active ? '#fc5200' : 'white'};
  color: ${props => props.active ? 'white' : '#666'};
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    border-color: #fc5200;
    color: ${props => props.active ? 'white' : '#fc5200'};
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const ExportSection = styled.div`
  padding: 32px;
`;

export const ExportTitle = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px 0;
`;

export const ExportDescription = styled.p`
  font-size: 14px;
  color: #666;
  margin: 0 0 24px 0;
`;

export const ExportOptions = styled.div`
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const ExportRow = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
`;

export const ExportLabel = styled.label`
  font-size: 14px;
  font-weight: 500;
  color: #333;
  min-width: 100px;
`;

export const Select = styled.select`
  padding: 10px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  font-size: 14px;
  min-width: 200px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:focus {
    outline: none;
    border-color: #fc5200;
  }
`;

export const FormatCards = styled.div`
  display: flex;
  gap: 12px;
`;

export const FormatCard = styled.button`
  padding: 16px 24px;
  border: 2px solid ${props => props.selected ? '#fc5200' : '#e5e5e5'};
  background: ${props => props.selected ? '#fff7ed' : 'white'};
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 100px;

  &:hover {
    border-color: #fc5200;
  }
`;

export const FormatIcon = styled.div`
  font-size: 24px;
`;

export const FormatName = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${props => props.selected ? '#fc5200' : '#333'};
`;

export const Checkbox = styled.label`
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
  cursor: pointer;

  input {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }
`;

export const ExportButton = styled.button`
  padding: 14px 28px;
  background: #fc5200;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;

  &:hover {
    background: #e04800;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
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

export const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: #666;
`;

export const EmptyIcon = styled.div`
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
`;

export const EmptyText = styled.p`
  font-size: 16px;
  margin: 0;
`;
