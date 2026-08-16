package com.strava.socialservice.user;

import com.strava.socialservice.domain.UserCache;
import com.strava.socialservice.domain.UserCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read side of the user_cache projection: turns cached rows into the
 * BaseUserResponse embedded in feed/comment payloads. A cache miss (user
 * event not yet consumed) degrades to a "User #id" author instead of failing
 * the request — main would 404 on a truly nonexistent user, but through the
 * gateway the authenticated id always exists, so a miss here only ever means
 * projection lag.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserCacheRepository userCacheRepository;

    @Transactional(readOnly = true)
    public BaseUserResponse userFor(Long userId) {
        return usersFor(List.of(userId)).get(userId);
    }

    /** Batch lookup — one query per response page, never per row. */
    @Transactional(readOnly = true)
    public Map<Long, BaseUserResponse> usersFor(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserCache> cached = userCacheRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserCache::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), id -> toResponse(id, cached.get(id))));
    }

    /** "First Last" for event payloads (liker/comment author), with the same "User #id" fallback. */
    @Transactional(readOnly = true)
    public String displayNameFor(Long userId) {
        BaseUserResponse user = userFor(userId);
        return user.getFullName();
    }

    private BaseUserResponse toResponse(Long userId, UserCache cache) {
        BaseUserResponse response = new BaseUserResponse();
        response.setId(userId);
        if (cache != null) {
            response.setFirstName(cache.getFirstName());
            response.setLastName(cache.getLastName());
            response.setAvatar(cache.getAvatar());
        } else {
            // fullName then renders as "User #<id>" — same fallback the old feed used.
            log.debug("user_cache miss for userId={} — degrading to placeholder author", userId);
            response.setFirstName("User");
            response.setLastName("#" + userId);
        }
        return response;
    }
}
