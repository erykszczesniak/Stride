import styled, { css } from "styled-components";

export const Container = styled.div`
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 16px;
`;

export const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
`;

export const PageTitle = styled.h1`
  font-size: 28px;
  font-weight: 800;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 4px;
`;

export const PageDescription = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0;
`;

export const ExportButtons = styled.div`
  display: flex;
  gap: 8px;
`;

export const ExportButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgCard};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
`;

export const MainLayout = styled.div`
  display: flex;
  gap: 20px;

  @media (max-width: 768px) {
    flex-direction: column;
  }
`;

export const FieldsPanel = styled.div`
  width: 220px;
  flex-shrink: 0;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  padding: 16px;
  height: fit-content;
  position: sticky;
  top: 20px;

  @media (max-width: 768px) {
    width: 100%;
    position: static;
  }
`;

export const FieldsPanelTitle = styled.h3`
  font-size: 14px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;

export const FieldItem = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }
`;

export const FieldCheckbox = styled.div`
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 2px solid ${({ $checked }) => ($checked ? "#fc4c02" : "#94a3b8")};
  background: ${({ $checked }) => ($checked ? "#fc4c02" : "transparent")};
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.15s;
  flex-shrink: 0;
`;

export const FieldLabel = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.textPrimary};
`;

export const ReportArea = styled.div`
  flex: 1;
  min-width: 0;
`;

export const DateBar = styled.div`
  display: flex;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  padding: 16px;
`;

export const DateLabel = styled.label`
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.textSecondary};
  margin-bottom: 4px;
`;

export const DateInput = styled.input`
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 13px;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

export const Presets = styled.div`
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
`;

export const PresetButton = styled.button`
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid ${({ theme }) => theme.border};
  background: transparent;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }
`;

export const ReportTable = styled.table`
  width: 100%;
  border-collapse: collapse;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  overflow: hidden;
`;

export const TableHead = styled.thead`
  background: ${({ theme }) => theme.bgSecondary};
`;

export const TableBody = styled.tbody``;

export const Th = styled.th`
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 700;
  color: ${({ theme }) => theme.textSecondary};
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
  border-bottom: 2px solid ${({ theme }) => theme.border};
  cursor: pointer;
  user-select: none;
  transition: color 0.15s;

  &:hover {
    color: #fc4c02;
  }

  ${({ $active }) =>
    $active &&
    css`
      color: #fc4c02;
    `}
`;

export const SortArrow = styled.span`
  margin-left: 4px;
  font-size: 10px;
  vertical-align: middle;
`;

export const Td = styled.td`
  padding: 10px 16px;
  font-size: 13px;
  color: ${({ theme }) => theme.textPrimary};
  border-bottom: 1px solid ${({ theme }) => theme.border};
  white-space: nowrap;
`;

export const SummaryRow = styled.tr`
  background: ${({ theme }) => theme.primaryLight};
  font-weight: 700;

  td {
    color: #fc4c02;
    font-weight: 700;
  }
`;

export const EmptyReport = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: ${({ theme }) => theme.textMuted};
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  text-align: center;

  p {
    margin-top: 16px;
    font-size: 14px;
  }
`;

/* Drag handle for reorderable fields */
export const DragHandle = styled.span`
  display: flex;
  align-items: center;
  cursor: grab;
  color: ${({ theme }) => theme.textMuted};
  transition: color 0.15s;
  flex-shrink: 0;

  &:hover {
    color: #fc4c02;
  }

  &:active {
    cursor: grabbing;
  }
`;

export const DraggableFieldItem = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  border: 1px solid transparent;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
  }

  ${({ $isDragging }) =>
    $isDragging &&
    css`
      opacity: 0.4;
      border: 1px dashed #fc4c02;
    `}

  ${({ $isDragOver }) =>
    $isDragOver &&
    css`
      border-top: 2px solid #fc4c02;
    `}
`;

/* Activity type filter dropdown */
export const FilterSelect = styled.select`
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 13px;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

/* Template UI */
export const TemplateBar = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
`;

export const TemplateButton = styled.button`
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid ${({ theme }) => theme.border};
  background: transparent;
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:hover {
    border-color: #fc4c02;
    color: #fc4c02;
  }
`;

export const TemplateSelect = styled.select`
  padding: 6px 12px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 13px;
  cursor: pointer;
  min-width: 140px;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

export const TemplateDeleteBtn = styled.button`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: ${({ theme }) => theme.textMuted};
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  transition: all 0.15s;

  &:hover {
    background: #fc4c02;
    color: white;
  }
`;

/* Save template modal/dialog */
export const ModalOverlay = styled.div`
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

export const ModalContent = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 12px;
  padding: 24px;
  width: 360px;
  max-width: 90vw;
`;

export const ModalTitle = styled.h3`
  font-size: 16px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin: 0 0 16px;
`;

export const ModalInput = styled.input`
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 14px;
  box-sizing: border-box;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

export const ModalActions = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
`;

export const ModalButton = styled.button`
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  ${({ $primary }) =>
    $primary
      ? css`
          background: #fc4c02;
          color: white;
          border: 1px solid #fc4c02;

          &:hover {
            background: #e04400;
          }
        `
      : css`
          background: transparent;
          color: ${({ theme }) => theme.textPrimary};
          border: 1px solid ${({ theme }) => theme.border};

          &:hover {
            border-color: #fc4c02;
            color: #fc4c02;
          }
        `}
`;

/* Bulk action toolbar */
export const ActionToolbar = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid #fc4c02;
  border-radius: 10px;
  flex-wrap: wrap;
`;

export const ActionToolbarText = styled.span`
  font-size: 13px;
  font-weight: 600;
  color: #fc4c02;
  margin-right: auto;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;

  ${({ $danger }) =>
    $danger
      ? css`
          border: 1px solid #ef4444;
          background: transparent;
          color: #ef4444;

          &:hover {
            background: #ef4444;
            color: white;
          }
        `
      : css`
          border: 1px solid ${({ theme }) => theme.border};
          background: transparent;
          color: ${({ theme }) => theme.textPrimary};

          &:hover {
            border-color: #fc4c02;
            color: #fc4c02;
          }
        `}
`;

export const ActionSelect = styled.select`
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid ${({ theme }) => theme.border};
  background: ${({ theme }) => theme.bgInput};
  color: ${({ theme }) => theme.textPrimary};
  font-size: 12px;
  cursor: pointer;

  &:focus {
    outline: none;
    border-color: #fc4c02;
  }
`;

/* Row checkbox */
export const RowCheckbox = styled.div`
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 2px solid ${({ $checked }) => ($checked ? "#fc4c02" : "#94a3b8")};
  background: ${({ $checked }) => ($checked ? "#fc4c02" : "transparent")};
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  transition: all 0.15s;
  flex-shrink: 0;
  cursor: pointer;
`;

export const SelectedFieldsList = styled.div`
  margin-top: 12px;
  border-top: 1px solid ${({ theme }) => theme.border};
  padding-top: 12px;
`;

export const SelectedFieldsTitle = styled.h4`
  font-size: 12px;
  font-weight: 700;
  color: ${({ theme }) => theme.textSecondary};
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
`;
