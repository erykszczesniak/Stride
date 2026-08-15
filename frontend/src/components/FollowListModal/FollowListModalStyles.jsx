import styled, { keyframes } from "styled-components";

// The SearchModal entrance, so every overlay in the app opens the same way.
const fadeIn = keyframes`
  from { opacity: 0; }
  to { opacity: 1; }
`;

const riseIn = keyframes`
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
`;

export const Overlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: ${fadeIn} 0.15s ease-out;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Modal = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.border};
  padding: 20px;
  width: 100%;
  max-width: 420px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 16px 48px ${({ theme }) => theme.shadowMedium},
    0 0 0 1px ${({ theme }) => theme.border};
  animation: ${riseIn} 0.2s cubic-bezier(0.2, 0.9, 0.3, 1);

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
`;

export const Title = styled.h3`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0;
`;

export const CloseButton = styled.button`
  background: none;
  border: none;
  cursor: pointer;
  color: ${({ theme }) => theme.textSecondary};
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
  }
`;

export const List = styled.div`
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const Row = styled.button`
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 8px;
  background: none;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  /* Rows cascade in after the list loads; the delay comes inline per row. */
  animation: ${fadeIn} 0.2s ease-out backwards;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
  }

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Avatar = styled.img`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
`;

export const AvatarInitials = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: #fc4c02;
`;

export const NameColumn = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 0;
`;

export const Name = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const City = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
`;

export const StateMessage = styled.div`
  padding: 24px 8px;
  text-align: center;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
`;
