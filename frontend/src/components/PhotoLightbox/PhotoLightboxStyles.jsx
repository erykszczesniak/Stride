import styled from "styled-components";

export const Overlay = styled.div`
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.88);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: zoom-out;
`;

export const LightboxImage = styled.img`
  max-width: 92vw;
  max-height: 88vh;
  object-fit: contain;
  border-radius: 8px;
  cursor: default;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.6);
`;

const roundButton = `
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    background: rgba(255, 255, 255, 0.25);
  }
`;

export const CloseButton = styled.button`
  ${roundButton}
  position: absolute;
  top: 20px;
  right: 24px;
  width: 40px;
  height: 40px;
`;

export const NavButton = styled.button`
  ${roundButton}
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  ${({ $side }) => ($side === "left" ? "left: 24px;" : "right: 24px;")}
  width: 44px;
  height: 44px;
`;

export const Counter = styled.div`
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  font-weight: 500;
`;
