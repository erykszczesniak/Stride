package com.strava.userservice.internal;

import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.exception.ApiRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.strava.userservice.common.ErrorMessages.USER_NOT_FOUND;

/**
 * Service-to-service API. NOT routed by the gateway — {@code /internal/**} is
 * cluster-only, enforced by the gateway route allowlist plus a k8s NetworkPolicy.
 * The response shapes are contracts other services build against — do not change them.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserRepository userRepository;
    private final UserFollowRepository followRepository;

    /** Batch summary lookup, e.g. for social-feed hydration. Unknown ids are simply omitted. */
    @GetMapping
    public List<InternalUserSummary> getUsers(@RequestParam List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIdIn(ids).stream()
                .map(InternalUserSummary::from)
                .toList();
    }

    /** Privacy check used by activity/social services to decide feed/profile visibility. */
    @GetMapping("/{id}/visibility")
    public UserVisibilityResponse getVisibility(
            @PathVariable Long id,
            @RequestParam(required = false) Long viewerId) {
        User user = getUser(id);
        boolean viewerFollows = viewerId != null
                && followRepository.existsByUserIdAndFollowerId(id, viewerId);
        return new UserVisibilityResponse(Boolean.TRUE.equals(user.getIsPrivate()), viewerFollows);
    }

    /** Contact + notification preferences for notification-service. */
    @GetMapping("/{id}/notification-profile")
    public NotificationProfileResponse getNotificationProfile(@PathVariable Long id) {
        return NotificationProfileResponse.from(getUser(id));
    }

    /**
     * Users opted into (and eligible for) the weekly summary digest. Returned as a
     * classic Spring {@code Page} JSON structure ({@code content}, {@code totalPages},
     * ...) — notification-service pages through it batch by batch.
     */
    @GetMapping("/weekly-summary-recipients")
    public Page<NotificationProfileResponse> getWeeklySummaryRecipients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return userRepository.findByWeeklySummaryTrueAndActiveTrue(PageRequest.of(page, size))
                .map(NotificationProfileResponse::from);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
