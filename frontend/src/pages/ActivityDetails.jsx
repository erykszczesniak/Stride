import React, { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { FiArrowLeft, FiHeart, FiMessageCircle, FiShare2, FiMapPin, FiClock, FiSend, FiEdit2, FiTrash2, FiX, FiCheck } from "react-icons/fi";
import { FaRunning, FaBicycle, FaSwimmer, FaHiking } from "react-icons/fa";
import Layout from "../components/Layout/Layout";
import ActivityService from "../api/ActivityService";
import CommentService from "../api/CommentService";
import { LoadingSpinner } from "./DashboardStyles";
import styled from "styled-components";

const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #666;
  text-decoration: none;
  font-size: 14px;
  margin-bottom: 24px;

  &:hover {
    color: #fc4c02;
  }
`;

const ActivityHeader = styled.div`
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
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
  color: #333;
`;

const ActivityTime = styled.p`
  font-size: 13px;
  color: #666;
`;

const Title = styled.h1`
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
`;

const Description = styled.p`
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 16px;
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
`;

const StatItem = styled.div`
  text-align: center;
`;

const StatValue = styled.p`
  font-size: 24px;
  font-weight: 700;
  color: #333;
`;

const StatLabel = styled.p`
  font-size: 12px;
  color: #666;
  text-transform: uppercase;
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
  color: #666;
  font-size: 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    background-color: #f5f5f5;
  }
`;

const CommentsSection = styled.div`
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
`;

const CommentsSectionTitle = styled.h2`
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
`;

const CommentForm = styled.form`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
`;

const CommentInput = styled.input`
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 24px;
  font-size: 14px;
  transition: all 0.2s;

  &:focus {
    outline: none;
    border-color: #fc4c02;
    box-shadow: 0 0 0 3px rgba(252, 76, 2, 0.1);
  }

  &::placeholder {
    color: #999;
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
  background: #f7f7f7;
  border-radius: 12px;
  padding: 12px 16px;
`;

const CommentHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
`;

const CommentAuthor = styled.span`
  font-weight: 600;
  color: #333;
  font-size: 14px;
`;

const CommentTime = styled.span`
  font-size: 12px;
  color: #999;
`;

const CommentText = styled.p`
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin: 0;
`;

const CommentActions = styled.div`
  display: flex;
  gap: 8px;
  margin-top: 8px;
`;

const CommentActionButton = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background: #e5e5e5;
    color: ${props => props.danger ? '#dc2626' : '#fc4c02'};
  }
`;

const EditCommentInput = styled.input`
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #fc4c02;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 8px;

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
  color: #999;
  font-size: 14px;
  padding: 20px 0;
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
  const [loading, setLoading] = useState(true);
  const [activity, setActivity] = useState(null);
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editContent, setEditContent] = useState("");
  const currentUserId = 1;

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        setLoading(true);
        const res = await ActivityService.getActivityById(id, currentUserId);
        setActivity(res.data);
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
        setComments(res.data || []);
      } catch (error) {
        console.error("Error fetching comments:", error);
      }
    };

    if (id) {
      fetchComments();
    }
  }, [id]);

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.trim() || submitting) return;

    setSubmitting(true);
    try {
      const res = await CommentService.createComment(id, newComment.trim(), currentUserId);
      setComments([...comments, res.data]);
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
      const res = await CommentService.updateComment(id, commentId, editContent.trim(), currentUserId);
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
      await CommentService.deleteComment(id, commentId, currentUserId);
      setComments(comments.filter(c => c.id !== commentId));
    } catch (error) {
      console.error("Error deleting comment:", error);
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
          <ActionButton>
            <FiHeart size={18} />
            {activity.likesCount || 0} Likes
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
                      {comment.user?.id === currentUserId && (
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
