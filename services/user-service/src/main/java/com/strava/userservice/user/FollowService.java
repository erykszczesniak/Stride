package com.strava.userservice.user;

import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollow;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.exception.BadRequestException;
import com.strava.userservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserRepository users;
    private final UserFollowRepository follows;

    @Transactional
    public void follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        if (!users.existsById(targetUserId)) {
            throw new NotFoundException("User " + targetUserId + " not found");
        }
        if (follows.existsByUserIdAndFollowerId(targetUserId, followerId)) {
            return; // idempotent
        }
        follows.save(UserFollow.builder()
                .userId(targetUserId)
                .followerId(followerId)
                .build());
    }

    @Transactional
    public void unfollow(Long followerId, Long targetUserId) {
        follows.deleteByUserIdAndFollowerId(targetUserId, followerId);
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> followers(Long userId, Pageable pageable) {
        return follows.findAllByUserId(userId, pageable)
                .map(f -> users.findById(f.getFollowerId())
                        .map(UserSummaryDto::from)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryDto> following(Long userId, Pageable pageable) {
        return follows.findAllByFollowerId(userId, pageable)
                .map(f -> users.findById(f.getUserId())
                        .map(UserSummaryDto::from)
                        .orElse(null));
    }

    public record UserSummaryDto(Long id, String firstName, String lastName, String avatar) {
        public static UserSummaryDto from(User u) {
            return new UserSummaryDto(u.getId(), u.getFirstName(), u.getLastName(), u.getAvatar());
        }
    }
}
