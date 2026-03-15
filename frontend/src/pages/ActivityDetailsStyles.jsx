import styled from "styled-components";
import { Link } from "react-router-dom";

export const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.textSecondary};
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;
  transition: color 0.3s ease;

  &:hover {
    color: ${({ theme }) => theme.primary};
  }
`;

export const ActivityHeader = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const UserSection = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

export const Avatar = styled.div`
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #fc4c02;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
`;

export const UserInfo = styled.div``;

export const UserName = styled.p`
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const ActivityTime = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

export const Title = styled.h1`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
  transition: color 0.3s ease;
`;

export const Description = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin-bottom: 16px;
  transition: color 0.3s ease;
`;

export const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid ${({ theme }) => theme.border};
  border-bottom: 1px solid ${({ theme }) => theme.border};
  margin-bottom: 16px;
  transition: border-color 0.3s ease;
`;

export const StatItem = styled.div`
  text-align: center;
`;

export const StatValue = styled.p`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

export const StatLabel = styled.p`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  text-transform: uppercase;
  transition: color 0.3s ease;
`;

export const ActionsBar = styled.div`
  display: flex;
  gap: 16px;
`;

export const ActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background-color: ${({ theme }) => theme.bgHover};
  }
`;

export const MapSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const MapSectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.3s ease;
`;

export const CommentsSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

export const CommentsSectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.3s ease;
`;

export const CommentForm = styled.form`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
  transition: border-color 0.3s ease;
`;

export const CommentInput = styled.input`
  flex: 1;
  padding: 12px 16px;
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 24px;
  font-size: 14px;
  background: ${({ theme }) => theme.bgSecondary};
  color: ${({ theme }) => theme.textPrimary};
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: ${({ theme }) => theme.primary};
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: ${({ theme }) => theme.textMuted};
  }
`;

export const CommentSubmitButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border: none;
  background: #fc4c02;
  color: white;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: #e04402;
  }

  &:disabled {
    background: #ccc;
    cursor: not-allowed;
  }
`;

export const CommentsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

export const CommentItem = styled.div`
  display: flex;
  gap: 12px;
`;

export const CommentAvatar = styled.div`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fc5200 0%, #ff7746 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }
`;

export const CommentContent = styled.div`
  flex: 1;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  padding: 12px 16px;
  transition: background-color 0.3s ease;
`;

export const CommentHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
`;

export const CommentAuthor = styled.span`
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  font-size: 14px;
  transition: color 0.3s ease;
`;

export const CommentTime = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textMuted};
  transition: color 0.3s ease;
`;

export const CommentText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1.5;
  margin: 0;
  transition: color 0.3s ease;
`;

export const CommentActions = styled.div`
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
  flex-wrap: wrap;
`;

export const ReactionBar = styled.div`
  display: flex;
  gap: 4px;
  margin-top: 8px;
  flex-wrap: wrap;
`;

export const ReactionBadge = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid
    ${(props) => (props.active ? props.theme.primary : props.theme.border)};
  background: ${(props) =>
    props.active ? "rgba(252, 76, 2, 0.1)" : props.theme.bgCard};
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: ${({ theme }) => theme.primary};
    background: rgba(252, 76, 2, 0.05);
  }

  span.emoji {
    font-size: 14px;
  }

  span.count {
    color: ${({ theme }) => theme.textSecondary};
    font-weight: 500;
  }
`;

export const AddReactionButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px dashed ${({ theme }) => theme.border};
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  transition: all 0.2s;
  position: relative;

  &:hover {
    border-color: ${({ theme }) => theme.primary};
    background: rgba(252, 76, 2, 0.05);
  }
`;

export const EmojiPicker = styled.div`
  position: absolute;
  bottom: 100%;
  left: 0;
  background: ${({ theme }) => theme.bgCard};
  border: 1px solid ${({ theme }) => theme.border};
  border-radius: 8px;
  padding: 8px;
  display: flex;
  gap: 4px;
  box-shadow: 0 4px 12px ${({ theme }) => theme.shadow};
  z-index: 10;
`;

export const EmojiOption = styled.button`
  background: transparent;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
    transform: scale(1.2);
  }
`;

export const CommentActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: ${({ theme }) => theme.textSecondary};
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background: ${({ theme }) => theme.bgHover};
    color: ${(props) => (props.danger ? "#dc2626" : props.theme.primary)};
  }
`;

export const EditCommentInput = styled.input`
  width: 100%;
  padding: 8px 12px;
  border: 1px solid ${({ theme }) => theme.primary};
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 8px;
  background: ${({ theme }) => theme.bgSecondary};
  color: ${({ theme }) => theme.textPrimary};

  &:focus {
    outline: none;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }
`;

export const EditActions = styled.div`
  display: flex;
  gap: 8px;
`;

export const EditButton = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;

  &.save {
    background: #fc4c02;
    color: white;

    &:hover {
      background: #e04402;
    }
  }

  &.cancel {
    background: ${({ theme }) => theme.bgHover};
    color: ${({ theme }) => theme.textSecondary};

    &:hover {
      background: ${({ theme }) => theme.border};
    }
  }
`;

export const NoComments = styled.p`
  text-align: center;
  color: ${({ theme }) => theme.textMuted};
  font-size: 14px;
  padding: 20px 0;
  transition: color 0.3s ease;
`;
