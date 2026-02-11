-- Comment Reactions table
CREATE TABLE comment_reactions (
    id BIGSERIAL PRIMARY KEY,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_comment_reaction_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_reaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_comment_reaction UNIQUE (comment_id, user_id, emoji)
);

CREATE INDEX idx_comment_reactions_comment ON comment_reactions(comment_id);
CREATE INDEX idx_comment_reactions_user ON comment_reactions(user_id);
