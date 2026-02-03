import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { FiArrowLeft, FiHeart, FiMessageCircle, FiShare2, FiMapPin, FiClock, FiSend, FiEdit2, FiTrash2, FiX, FiCheck } from "react-icons/fi";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import Layout from "../components/Layout/Layout";
import ActivityService from "../api/ActivityService";
import CommentService from "../api/CommentService";
import { useAuth } from "../context/AuthContext";
import { LoadingSpinner } from "./DashboardStyles";
import styled from "styled-components";

const BackLink = styled(Link)`
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

const ActivityHeader = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

const UserSection = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
`;

const Avatar = styled.div`
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

const UserInfo = styled.div``;

const UserName = styled.p`
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

const ActivityTime = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.textSecondary};
  transition: color 0.3s ease;
`;

const Title = styled.h1`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 8px;
  transition: color 0.3s ease;
`;

const Description = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textSecondary};
  line-height: 1.6;
  margin-bottom: 16px;
  transition: color 0.3s ease;
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid ${({ theme }) => theme.border};
  border-bottom: 1px solid ${({ theme }) => theme.border};
  margin-bottom: 16px;
  transition: border-color 0.3s ease;
`;

const StatItem = styled.div`
  text-align: center;
`;

const StatValue = styled.p`
  font-size: 24px;
  font-weight: 700;
  color: ${({ theme }) => theme.textPrimary};
  transition: color 0.3s ease;
`;

const StatLabel = styled.p`
  font-size: 12px;
  color: ${({ theme }) => theme.textSecondary};
  text-transform: uppercase;
  transition: color 0.3s ease;
`;

const ActionsBar = styled.div`
  display: flex;
  gap: 16px;
`;

const ActionButton = styled.button`
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

const CommentsSection = styled.div`
  background: ${({ theme }) => theme.bgCard};
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px ${({ theme }) => theme.shadow};
  border: 1px solid ${({ theme }) => theme.border};
  transition: all 0.3s ease;
`;

const CommentsSectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color 0.3s ease;
`;

const CommentForm = styled.form`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid ${({ theme }) => theme.border};
  transition: border-color 0.3s ease;
`;

const CommentInput = styled.input`
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

const CommentSubmitButton = styled.button`
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

const CommentsList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
`;

const CommentItem = styled.div`
  display: flex;
  gap: 12px;
`;

const CommentAvatar = styled.div`
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

const CommentContent = styled.div`
  flex: 1;
  background: ${({ theme }) => theme.bgHover};
  border-radius: 12px;
  padding: 12px 16px;
  transition: background-color 0.3s ease;
`;

const CommentHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
`;

const CommentAuthor = styled.span`
  font-weight: 600;
  color: ${({ theme }) => theme.textPrimary};
  font-size: 14px;
  transition: color 0.3s ease;
`;

const CommentTime = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.textMuted};
  transition: color 0.3s ease;
`;

const CommentText = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.textPrimary};
  line-height: 1.5;
  margin: 0;
  transition: color 0.3s ease;
`;

const CommentActions = styled.div`
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
  flex-wrap: wrap;
`;

const ReactionBar = styled.div`
  display: flex;
  gap: 4px;
  margin-top: 8px;
  flex-wrap: wrap;
`;

const ReactionBadge = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid ${props => props.active ? props.theme.primary : props.theme.border};
  background: ${props => props.active ? 'rgba(252, 76, 2, 0.1)' : props.theme.bgCard};
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

const AddReactionButton = styled.button`
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

const EmojiPicker = styled.div`
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

const EmojiOption = styled.button`
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

const CommentActionButton = styled.button`
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
    color: ${props => props.danger ? '#dc2626' : props.theme.primary};
  }
`;

const EditCommentInput = styled.input`
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

const EditActions = styled.div`
  display: flex;
  gap: 8px;
`;

const EditButton = styled.button`
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
    background: #e5e5e5;
    color: #666;

    &:hover {
      background: #d5d5d5;
    }
  }
`;

const NoComments = styled.p`
  text-align: center;
  color: ${({ theme }) => theme.textMuted};
  font-size: 14px;
  padding: 20px 0;
  transition: color 0.3s ease;
`;

const activityIcons = {
  RUN: <FaRunning size={20} />,
  RIDE: <FaBicycle size={20} />,
  SWIM: <FaSwimmer size={20} />,
  HIKE: <FaHiking size={20} />,
};

const formatDuration = (seconds) => {
  if (!seconds) return "0m";
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
};

const ActivityDetails = () => {
  const { id } = useParams();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [activity, setActivity] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editContent, setEditContent] = useState("");
  const [likedByMe, setLikedByMe] = useState(false);
  const [likesCount, setLikesCount] = useState(0);
  const [commentReactions, setCommentReactions] = useState({});
  const [userReactions, setUserReactions] = useState({});
  const [showEmojiPicker, setShowEmojiPicker] = useState(null);

  const availableEmojis = ['👍', '❤️', '🔥', '👏', '💪', '🎉'];

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        setLoading(true);
        const res = await ActivityService.getActivityById(id);
        setActivity(res.data);
        setLikedByMe(res.data.likedByCurrentUser || false);
        setLikesCount(res.data.likesCount || 0);
      } catch (error) {
        console.error("Error fetching activity:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchActivity();
  }, [id]);

  useEffect(() => {
    const fetchComments = async () => {
      try {
        const res = await CommentService.getActivityComments(id);
        const commentsData = res.data || [];
        setComments(commentsData);

        // Fetch reactions for each comment
        for (const comment of commentsData) {
          fetchReactions(comment.id);
        }
      } catch (error) {
        console.error("Error fetching comments:", error);
      }
    };

    if (id) {
      fetchComments();
    }
  }, [id]);

  const handleLike = async () => {
    try {
      if (likedByMe) {
        await ActivityService.unlikeActivity(id);
        setLikedByMe(false);
        setLikesCount(prev => prev - 1);
      } else {
        await ActivityService.likeActivity(id);
        setLikedByMe(true);
        setLikesCount(prev => prev + 1);
      }
    } catch (error) {
      console.error("Error toggling like:", error);
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || submitting) return;

    setSubmitting(true);
    try {
      const res = await CommentService.createComment(id, newComment.trim());
      const newCommentData = res.data;
      setComments([...comments, newCommentData]);
      setCommentReactions(prev => ({ ...prev, [newCommentData.id]: {} }));
      setUserReactions(prev => ({ ...prev, [newCommentData.id]: [] }));
      setNewComment("");
    } catch (error) {
      console.error("Error creating comment:", error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditComment = (comment) => {
    setEditingCommentId(comment.id);
    setEditContent(comment.content);
  };

  const handleCancelEdit = () => {
    setEditingCommentId(null);
    setEditContent("");
  };

  const handleSaveEdit = async (commentId) => {
    if (!editContent.trim()) return;

    try {
      const res = await CommentService.updateComment(id, commentId, editContent.trim());
      setComments(comments.map(c => c.id === commentId ? res.data : c));
      setEditingCommentId(null);
      setEditContent("");
    } catch (error) {
      console.error("Error updating comment:", error);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm("Are you sure you want to delete this comment?")) return;

    try {
      await CommentService.deleteComment(id, commentId);
      setComments(comments.filter(c => c.id !== commentId));
    } catch (error) {
      console.error("Error deleting comment:", error);
    }
  };

  const fetchReactions = async (commentId) => {
    try {
      const res = await CommentService.getReactions(id, commentId);
      setCommentReactions(prev => ({ ...prev, [commentId]: res.data.reactions || {} }));
      setUserReactions(prev => ({ ...prev, [commentId]: res.data.userReactions || [] }));
    } catch (error) {
      console.error("Error fetching reactions:", error);
    }
  };

  const handleAddReaction = async (commentId, emoji) => {
    try {
      const res = await CommentService.addReaction(id, commentId, emoji);
      setCommentReactions(prev => ({ ...prev, [commentId]: res.data.reactions || {} }));
      setUserReactions(prev => ({ ...prev, [commentId]: res.data.userReactions || [] }));
      setShowEmojiPicker(null);
    } catch (error) {
      console.error("Error adding reaction:", error);
    }
  };

  const handleRemoveReaction = async (commentId, emoji) => {
    try {
      const res = await CommentService.removeReaction(id, commentId, emoji);
      setCommentReactions(prev => ({ ...prev, [commentId]: res.data.reactions || {} }));
      setUserReactions(prev => ({ ...prev, [commentId]: res.data.userReactions || [] }));
    } catch (error) {
      console.error("Error removing reaction:", error);
    }
  };

  const handleToggleReaction = (commentId, emoji) => {
    const userEmojis = userReactions[commentId] || [];
    if (userEmojis.includes(emoji)) {
      handleRemoveReaction(commentId, emoji);
    } else {
      handleAddReaction(commentId, emoji);
    }
  };

  const formatCommentTime = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  const getInitials = (firstName, lastName) => {
    return `${(firstName || "")[0] || ""}${(lastName || "")[0] || ""}`.toUpperCase();
  };

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  if (!activity) {
    return (
      <Layout>
        <p>Activity not found</p>
      </Layout>
    );
  }

  return (
    <Layout>
      <BackLink to="/activities">
        <FiArrowLeft size={18} />
        Back to Activities
      </BackLink>

      <ActivityHeader>
        <UserSection>
          <Avatar>
            {activity.user?.firstName?.charAt(0) || "U"}
          </Avatar>
          <UserInfo>
            <UserName>
              {activity.user?.firstName} {activity.user?.lastName}
            </UserName>
            <ActivityTime>
              {new Date(activity.createdAt).toLocaleDateString()}
            </ActivityTime>
          </UserInfo>
        </UserSection>

        <Title>{activity.title}</Title>
        {activity.description && <Description>{activity.description}</Description>}

        <StatsGrid>
          <StatItem>
            <StatValue>{activity.distance?.toFixed(2) || 0}</StatValue>
            <StatLabel>Distance (km)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{formatDuration(activity.duration)}</StatValue>
            <StatLabel>Duration</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.elevationGain || 0}</StatValue>
            <StatLabel>Elevation (m)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.calories || 0}</StatValue>
            <StatLabel>Calories</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.averageSpeed?.toFixed(1) || 0}</StatValue>
            <StatLabel>Avg Speed (km/h)</StatLabel>
          </StatItem>
          <StatItem>
            <StatValue>{activity.averageHeartRate || "-"}</StatValue>
            <StatLabel>Avg HR (bpm)</StatLabel>
          </StatItem>
        </StatsGrid>

        <ActionsBar>
          <ActionButton onClick={handleLike} style={{ color: likedByMe ? '#fc4c02' : '#666' }}>
            <FiHeart size={18} fill={likedByMe ? '#fc4c02' : 'none'} />
            {likesCount} Likes
          </ActionButton>
          <ActionButton>
            <FiMessageCircle size={18} />
            {comments.length} Comments
          </ActionButton>
          <ActionButton>
            <FiShare2 size={18} />
            Share
          </ActionButton>
        </ActionsBar>
      </ActivityHeader>

      <CommentsSection>
        <CommentsSectionTitle>
          <FiMessageCircle size={20} />
          Comments ({comments.length})
        </CommentsSectionTitle>

        <CommentForm onSubmit={handleSubmitComment}>
          <CommentInput
            type="text"
            placeholder="Write a comment..."
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
          />
          <CommentSubmitButton type="submit" disabled={!newComment.trim() || submitting}>
            <FiSend size={18} />
          </CommentSubmitButton>
        </CommentForm>

        <CommentsList>
          {comments.length === 0 ? (
            <NoComments>No comments yet. Be the first to comment!</NoComments>
          ) : (
            comments.map((comment) => (
              <CommentItem key={comment.id}>
                <CommentAvatar>
                  {comment.user?.avatar ? (
                    <img src={comment.user.avatar} alt="" />
                  ) : (
                    getInitials(comment.user?.firstName, comment.user?.lastName)
                  )}
                </CommentAvatar>
                <CommentContent>
                  <CommentHeader>
                    <CommentAuthor>
                      {comment.user?.firstName} {comment.user?.lastName}
                    </CommentAuthor>
                    <CommentTime>{formatCommentTime(comment.createdAt)}</CommentTime>
                  </CommentHeader>
                  {editingCommentId === comment.id ? (
                    <>
                      <EditCommentInput
                        type="text"
                        value={editContent}
                        onChange={(e) => setEditContent(e.target.value)}
                        autoFocus
                      />
                      <EditActions>
                        <EditButton className="save" onClick={() => handleSaveEdit(comment.id)}>
                          <FiCheck size={14} />
                          Save
                        </EditButton>
                        <EditButton className="cancel" onClick={handleCancelEdit}>
                          <FiX size={14} />
                          Cancel
                        </EditButton>
                      </EditActions>
                    </>
                  ) : (
                    <>
                      <CommentText>{comment.content}</CommentText>
                      <ReactionBar>
                        {Object.entries(commentReactions[comment.id] || {}).map(([emoji, count]) => (
                          <ReactionBadge
                            key={emoji}
                            active={(userReactions[comment.id] || []).includes(emoji)}
                            onClick={() => handleToggleReaction(comment.id, emoji)}
                          >
                            <span className="emoji">{emoji}</span>
                            <span className="count">{count}</span>
                          </ReactionBadge>
                        ))}
                        <AddReactionButton onClick={() => setShowEmojiPicker(showEmojiPicker === comment.id ? null : comment.id)}>
                          +
                          {showEmojiPicker === comment.id && (
                            <EmojiPicker onClick={(e) => e.stopPropagation()}>
                              {availableEmojis.map(emoji => (
                                <EmojiOption key={emoji} onClick={() => handleAddReaction(comment.id, emoji)}>
                                  {emoji}
                                </EmojiOption>
                              ))}
                            </EmojiPicker>
                          )}
                        </AddReactionButton>
                      </ReactionBar>
                      {comment.user?.id === user?.id && (
                        <CommentActions>
                          <CommentActionButton onClick={() => handleEditComment(comment)}>
                            <FiEdit2 size={12} />
                            Edit
                          </CommentActionButton>
                          <CommentActionButton danger onClick={() => handleDeleteComment(comment.id)}>
                            <FiTrash2 size={12} />
                            Delete
                          </CommentActionButton>
                        </CommentActions>
                      )}
                    </>
                  )}
                </CommentContent>
              </CommentItem>
            ))
          )}
        </CommentsList>
      </CommentsSection>
    </Layout>
  );
};

export default ActivityDetails;
