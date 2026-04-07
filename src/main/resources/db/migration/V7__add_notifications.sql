-- Create notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);
CREATE INDEX idx_notifications_recipient_unread ON notifications(recipient_id, is_read) WHERE is_read = FALSE;

-- Sample notifications for user 1 (Pan)
INSERT INTO notifications (recipient_id, sender_id, type, message, reference_id, reference_type, is_read, created_at) VALUES
(1, 2, 'LIKE', 'John Doe liked your activity "Morning Trail Run"', 1, 'ACTIVITY', FALSE, NOW() - INTERVAL '2 hours'),
(1, 3, 'LIKE', 'Jane Smith liked your activity "Morning Trail Run"', 1, 'ACTIVITY', FALSE, NOW() - INTERVAL '3 hours'),
(1, 2, 'COMMENT', 'John Doe commented on your activity "Morning Trail Run"', 1, 'ACTIVITY', FALSE, NOW() - INTERVAL '5 hours'),
(1, 3, 'COMMENT', 'Jane Smith commented on your activity "Morning Trail Run"', 1, 'ACTIVITY', TRUE, NOW() - INTERVAL '5 hours'),
(1, 2, 'LIKE', 'John Doe liked your activity "Weekend Long Ride"', 2, 'ACTIVITY', TRUE, NOW() - INTERVAL '1 day'),
(1, 3, 'FOLLOW', 'Jane Smith started following you', NULL, NULL, TRUE, NOW() - INTERVAL '2 days'),
(1, NULL, 'ACHIEVEMENT', 'You completed 50 km of running this week!', NULL, NULL, FALSE, NOW() - INTERVAL '6 hours'),
(1, NULL, 'SYSTEM', 'Welcome to Stride! Start tracking your activities.', NULL, NULL, TRUE, NOW() - INTERVAL '7 days');

-- Sample notifications for user 2 (John)
INSERT INTO notifications (recipient_id, sender_id, type, message, reference_id, reference_type, is_read, created_at) VALUES
(2, 1, 'LIKE', 'Pan Szczesniak liked your activity "Golden Gate Run"', 7, 'ACTIVITY', FALSE, NOW() - INTERVAL '1 hour'),
(2, 3, 'LIKE', 'Jane Smith liked your activity "Golden Gate Run"', 7, 'ACTIVITY', FALSE, NOW() - INTERVAL '2 hours'),
(2, 1, 'COMMENT', 'Pan Szczesniak commented on your activity "Golden Gate Run"', 7, 'ACTIVITY', FALSE, NOW() - INTERVAL '4 hours'),
(2, 1, 'FOLLOW', 'Pan Szczesniak started following you', NULL, NULL, TRUE, NOW() - INTERVAL '3 days'),
(2, NULL, 'ACHIEVEMENT', 'New personal best! You ran 12 km in one session.', 7, 'ACTIVITY', TRUE, NOW() - INTERVAL '5 days');

-- Sample notifications for user 3 (Jane)
INSERT INTO notifications (recipient_id, sender_id, type, message, reference_id, reference_type, is_read, created_at) VALUES
(3, 1, 'LIKE', 'Pan Szczesniak liked your activity "Central Park Long Run"', 13, 'ACTIVITY', FALSE, NOW() - INTERVAL '30 minutes'),
(3, 2, 'LIKE', 'John Doe liked your activity "Central Park Long Run"', 13, 'ACTIVITY', FALSE, NOW() - INTERVAL '1 hour'),
(3, 1, 'COMMENT', 'Pan Szczesniak commented on your activity "Central Park Long Run"', 13, 'ACTIVITY', FALSE, NOW() - INTERVAL '5 hours'),
(3, 2, 'COMMENT', 'John Doe commented on your activity "Central Park Long Run"', 13, 'ACTIVITY', TRUE, NOW() - INTERVAL '5 hours'),
(3, 1, 'FOLLOW', 'Pan Szczesniak started following you', NULL, NULL, TRUE, NOW() - INTERVAL '4 days'),
(3, NULL, 'GOAL_COMPLETED', 'You completed your weekly running goal of 80 km!', 7, 'GOAL', FALSE, NOW() - INTERVAL '12 hours'),
(3, NULL, 'ACHIEVEMENT', 'Marathon distance reached! You ran 32.2 km in one session.', 13, 'ACTIVITY', TRUE, NOW() - INTERVAL '6 days');

-- Sample notifications for user 4 (Admin)
INSERT INTO notifications (recipient_id, sender_id, type, message, reference_id, reference_type, is_read, created_at) VALUES
(4, 1, 'LIKE', 'Pan Szczesniak liked your activity "Thames Path Run"', 21, 'ACTIVITY', FALSE, NOW() - INTERVAL '2 hours'),
(4, 2, 'LIKE', 'John Doe liked your activity "Thames Path Run"', 21, 'ACTIVITY', FALSE, NOW() - INTERVAL '3 hours'),
(4, 1, 'COMMENT', 'Pan Szczesniak commented on your activity "Thames Path Run"', 21, 'ACTIVITY', FALSE, NOW() - INTERVAL '2 hours'),
(4, NULL, 'SYSTEM', 'System maintenance scheduled for this weekend.', NULL, NULL, FALSE, NOW() - INTERVAL '1 day');
