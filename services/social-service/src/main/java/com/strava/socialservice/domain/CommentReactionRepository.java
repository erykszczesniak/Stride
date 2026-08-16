package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByCommentIdAndUserIdAndEmoji(Long commentId, Long userId, String emoji);

    /** Same grouped-count shape as main's countReactionsByCommentId. */
    @Query("SELECT cr.emoji, COUNT(cr) FROM CommentReaction cr WHERE cr.commentId = :commentId GROUP BY cr.emoji")
    List<Object[]> countReactionsByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cr.emoji FROM CommentReaction cr WHERE cr.commentId = :commentId AND cr.userId = :userId")
    List<String> findUserReactionsByCommentId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM CommentReaction cr WHERE cr.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
