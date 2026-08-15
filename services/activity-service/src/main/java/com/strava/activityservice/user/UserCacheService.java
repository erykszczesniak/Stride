package com.strava.activityservice.user;

import com.strava.activityservice.domain.UserCache;
import com.strava.activityservice.domain.UserCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read side of the user_cache projection: turns cached rows into the
 * BaseUserResponse embedded in activity payloads. A cache miss (event not yet
 * consumed) degrades to an id-only author object instead of failing the
 * request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserCacheRepository userCacheRepository;

    @Transactional(readOnly = true)
    public BaseUserResponse userFor(Long userId) {
        return usersFor(java.util.List.of(userId)).get(userId);
    }

    /** Batch lookup — one query per response page, never per row. */
    @Transactional(readOnly = true)
    public Map<Long, BaseUserResponse> usersFor(Collection<Long> userIds) {
        Map<Long, UserCache> cached = userCacheRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserCache::getUserId, Function.identity()));
        return userIds.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), id -> toResponse(id, cached.get(id))));
    }

    private BaseUserResponse toResponse(Long userId, UserCache cache) {
        BaseUserResponse response = new BaseUserResponse();
        response.setId(userId);
        if (cache != null) {
            response.setFirstName(cache.getFirstName());
            response.setLastName(cache.getLastName());
            response.setAvatar(cache.getAvatar());
        } else {
            log.debug("user_cache miss for userId={} — returning id-only author", userId);
        }
        return response;
    }
}
