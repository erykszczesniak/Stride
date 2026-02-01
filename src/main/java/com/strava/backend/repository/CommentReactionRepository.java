package com.strava.backend.repository;

import com.strava.backend.domain.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    @Query("SELECT cr FROM CommentReaction cr WHERE cr.comment.id = :commentId")
    List<CommentReaction> findByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cr FROM CommentReaction cr WHERE cr.comment.id = :commentId AND cr.user.id = :userId AND cr.emoji = :emoji")
    Optional<CommentReaction> findByCommentIdAndUserIdAndEmoji(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId,
            @Param("emoji") String emoji);

    @Query("SELECT cr.emoji, COUNT(cr) FROM CommentReaction cr WHERE cr.comment.id = :commentId GROUP BY cr.emoji")
    List<Object[]> countReactionsByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cr.emoji FROM CommentReaction cr WHERE cr.comment.id = :commentId AND cr.user.id = :userId")
    List<String> findUserReactionsByCommentId(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
