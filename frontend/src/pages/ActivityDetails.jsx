import React, { useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  FiArrowLeft,
  FiHeart,
  FiMessageCircle,
  FiShare2,
  FiMapPin,
  FiSend,
  FiEdit2,
  FiTrash2,
  FiX,
  FiCheck,
  FiImage,
} from "react-icons/fi";
import Layout from "../components/Layout/Layout";
import ActivityService from "../api/ActivityService";
import CommentService from "../api/CommentService";
import ActivityMap from "../components/ActivityMap/ActivityMap";
import ShareModal from "../components/ShareModal/ShareModal";
import { useAuth } from "../context/AuthContext";
import { LoadingSpinner } from "./DashboardStyles";
import { formatDuration, getTimeAgo, getInitials } from "../utils/formatters";
import {
  BackLink,
  ActivityHeader,
  UserSection,
  Avatar,
  UserInfo,
  UserName,
  ActivityTime,
  Title,
  Description,
  StatsGrid,
  StatItem,
  StatValue,
  StatLabel,
  ActionsBar,
  ActionButton,
  MapSection,
  MapSectionTitle,
  CommentsSection,
  CommentsSectionTitle,
  CommentForm,
  CommentInput,
  CommentSubmitButton,
  CommentsList,
  CommentItem,
  CommentAvatar,
  CommentContent,
  CommentHeader,
  CommentAuthor,
  CommentTime,
  CommentText,
  CommentActions,
  ReactionBar,
  ReactionBadge,
  AddReactionButton,
  EmojiPicker,
  EmojiOption,
  CommentActionButton,
  EditCommentInput,
  EditActions,
  EditButton,
  NoComments,
  PhotoGallerySection,
  PhotoGallerySectionTitle,
  PhotoGrid,
  PhotoGridItem,
  PhotoGridImage,
} from "./ActivityDetailsStyles";

const ActivityDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
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
  const [showShareModal, setShowShareModal] = useState(false);
  const commentsSectionRef = useRef(null);

  const availableEmojis = ["😆", "👍", "❤️", "🔥", "👏", "💪", "🎉"];

  useEffect(() => {
    const fetchActivity = async () => {
      try {
        setLoading(true);
        const res = await ActivityService.getActivityById(id);
        setActivity(res.data);
        setLikedByMe(res.data.likedByCurrentUser || false);
        setLikesCount(res.data.likesCount || 0);
      } catch (error) {
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
      }
    };

    if (id) {
      fetchComments();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleLike = async () => {
    try {
      if (likedByMe) {
        await ActivityService.unlikeActivity(id);
        setLikedByMe(false);
        setLikesCount((prev) => prev - 1);
      } else {
        await ActivityService.likeActivity(id);
        setLikedByMe(true);
        setLikesCount((prev) => prev + 1);
      }
    } catch (error) {
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
      setCommentReactions((prev) => ({ ...prev, [newCommentData.id]: {} }));
      setUserReactions((prev) => ({ ...prev, [newCommentData.id]: [] }));
      setNewComment("");
    } catch (error) {
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
      const res = await CommentService.updateComment(
        id,
        commentId,
        editContent.trim(),
      );
      setComments(comments.map((c) => (c.id === commentId ? res.data : c)));
      setEditingCommentId(null);
      setEditContent("");
    } catch (error) {
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!window.confirm("Are you sure you want to delete this comment?"))
      return;

    try {
      await CommentService.deleteComment(id, commentId);
      setComments(comments.filter((c) => c.id !== commentId));
    } catch (error) {
    }
  };

  const fetchReactions = async (commentId) => {
    try {
      const res = await CommentService.getReactions(id, commentId);
      setCommentReactions((prev) => ({
        ...prev,
        [commentId]: res.data.reactions || {},
      }));
      setUserReactions((prev) => ({
        ...prev,
        [commentId]: res.data.userReactions || [],
      }));
    } catch (error) {
    }
  };

  const handleAddReaction = async (commentId, emoji) => {
    try {
      const res = await CommentService.addReaction(id, commentId, emoji);
      setCommentReactions((prev) => ({
        ...prev,
        [commentId]: res.data.reactions || {},
      }));
      setUserReactions((prev) => ({
        ...prev,
        [commentId]: res.data.userReactions || [],
      }));
      setShowEmojiPicker(null);
    } catch (error) {
    }
  };

  const handleRemoveReaction = async (commentId, emoji) => {
    try {
      const res = await CommentService.removeReaction(id, commentId, emoji);
      setCommentReactions((prev) => ({
        ...prev,
        [commentId]: res.data.reactions || {},
      }));
      setUserReactions((prev) => ({
        ...prev,
        [commentId]: res.data.userReactions || [],
      }));
    } catch (error) {
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

  if (loading) {
    return (
      <Layout>
        <LoadingSpinner />
      </Layout>
    );
  }

  const isOwner = user && activity?.user?.id === user.id;

  const handleDeleteActivity = async () => {
    if (!window.confirm(`Delete "${activity.title}"? This cannot be undone.`)) return;
    try {
      await ActivityService.deleteActivity(id);
      navigate('/activities');
    } catch (error) {
    }
  };

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
          <Avatar>{activity.user?.firstName?.charAt(0) || "U"}</Avatar>
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
        {activity.description && (
          <Description>{activity.description}</Description>
        )}

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
          <ActionButton
            onClick={handleLike}
            style={{ color: likedByMe ? "#fc4c02" : "#666" }}
          >
            <FiHeart size={18} fill={likedByMe ? "#fc4c02" : "none"} />
            {likesCount} Likes
          </ActionButton>
          <ActionButton
            onClick={() =>
              commentsSectionRef.current?.scrollIntoView({ behavior: "smooth" })
            }
          >
            <FiMessageCircle size={18} />
            {comments.length} Comments
          </ActionButton>
          <ActionButton onClick={() => setShowShareModal(true)}>
            <FiShare2 size={18} />
            Share
          </ActionButton>
          {isOwner && (
            <>
              <ActionButton onClick={() => navigate(`/activities/${id}/edit`)}>
                <FiEdit2 size={18} />
                Edit
              </ActionButton>
              <ActionButton onClick={handleDeleteActivity} style={{ color: "#ef4444" }}>
                <FiTrash2 size={18} />
                Delete
              </ActionButton>
            </>
          )}
        </ActionsBar>
      </ActivityHeader>

      <MapSection>
        <MapSectionTitle>
          <FiMapPin size={20} />
          Route Map
        </MapSectionTitle>
        <ActivityMap coordinates={activity.routePolyline} height="400px" />
      </MapSection>

      {activity.photos && activity.photos.length > 0 && (
        <PhotoGallerySection>
          <PhotoGallerySectionTitle>
            <FiImage size={20} />
            Photos ({activity.photos.length})
          </PhotoGallerySectionTitle>
          <PhotoGrid>
            {activity.photos.map((photo, index) => {
              const photoUrl = photo.startsWith('http')
                ? photo
                : `${process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080'}${photo}`;
              return (
                <PhotoGridItem key={index}>
                  <PhotoGridImage src={photoUrl} alt={`Activity photo ${index + 1}`} />
                </PhotoGridItem>
              );
            })}
          </PhotoGrid>
        </PhotoGallerySection>
      )}

      <CommentsSection ref={commentsSectionRef}>
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
          <CommentSubmitButton
            type="submit"
            disabled={!newComment.trim() || submitting}
          >
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
                    <CommentTime>
                      {getTimeAgo(comment.createdAt)}
                    </CommentTime>
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
                        <EditButton
                          className="save"
                          onClick={() => handleSaveEdit(comment.id)}
                        >
                          <FiCheck size={14} />
                          Save
                        </EditButton>
                        <EditButton
                          className="cancel"
                          onClick={handleCancelEdit}
                        >
                          <FiX size={14} />
                          Cancel
                        </EditButton>
                      </EditActions>
                    </>
                  ) : (
                    <>
                      <CommentText>{comment.content}</CommentText>
                      <ReactionBar>
                        {Object.entries(commentReactions[comment.id] || {}).map(
                          ([emoji, count]) => (
                            <ReactionBadge
                              key={emoji}
                              active={(
                                userReactions[comment.id] || []
                              ).includes(emoji)}
                              onClick={() =>
                                handleToggleReaction(comment.id, emoji)
                              }
                            >
                              <span className="emoji">{emoji}</span>
                              <span className="count">{count}</span>
                            </ReactionBadge>
                          ),
                        )}
                        <AddReactionButton
                          onClick={() =>
                            setShowEmojiPicker(
                              showEmojiPicker === comment.id
                                ? null
                                : comment.id,
                            )
                          }
                        >
                          +
                          {showEmojiPicker === comment.id && (
                            <EmojiPicker onClick={(e) => e.stopPropagation()}>
                              {availableEmojis.map((emoji) => (
                                <EmojiOption
                                  key={emoji}
                                  onClick={() =>
                                    handleAddReaction(comment.id, emoji)
                                  }
                                >
                                  {emoji}
                                </EmojiOption>
                              ))}
                            </EmojiPicker>
                          )}
                        </AddReactionButton>
                      </ReactionBar>
                      {comment.user?.id === user?.id && (
                        <CommentActions>
                          <CommentActionButton
                            onClick={() => handleEditComment(comment)}
                          >
                            <FiEdit2 size={12} />
                            Edit
                          </CommentActionButton>
                          <CommentActionButton
                            danger
                            onClick={() => handleDeleteComment(comment.id)}
                          >
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

      <ShareModal
        isOpen={showShareModal}
        onClose={() => setShowShareModal(false)}
        activityId={id}
      />
    </Layout>
  );
};

export default ActivityDetails;
