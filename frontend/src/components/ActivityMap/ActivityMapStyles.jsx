import styled from "styled-components";

export const MapWrapper = styled.div`
  width: 100%;
  height: ${({ $height }) => $height || "400px"};
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};

  .leaflet-container {
    width: 100%;
    height: 100%;
    background: ${({ theme }) => theme.bgCard};
  }

  .leaflet-control-attribution {
    background: ${({ theme }) => theme.bgCard} !important;
    color: ${({ theme }) => theme.textMuted} !important;
  }

  .leaflet-control-attribution a {
    color: ${({ theme }) => theme.primary} !important;
  }
`;

export const EmptyMapState = styled.div`
  width: 100%;
  height: ${({ $height }) => $height || "400px"};
  border-radius: 12px;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.textSecondary};
  gap: 12px;
`;

export const EmptyMapIcon = styled.div`
  font-size: 48px;
`;

export const EmptyMapText = styled.p`
  font-size: 14px;
  margin: 0;
`;
