package com.strava.socialservice.feed;

import com.strava.socialservice.domain.FeedItem;
import com.strava.socialservice.domain.FeedItemRepository;
import com.strava.socialservice.domain.UserCache;
import com.strava.socialservice.domain.UserCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedItemRepository feed;
    private final UserCacheRepository users;

    @Transactional(readOnly = true)
    public Page<FeedItemDto> globalFeed(Pageable pageable) {
        Page<FeedItem> page = feed.findAllByOrderByStartedAtDesc(pageable);
        return hydrate(page);
    }

    @Transactional(readOnly = true)
    public Page<FeedItemDto> userFeed(Long userId, Pageable pageable) {
        Page<FeedItem> page = feed.findAllByUserIdOrderByStartedAtDesc(userId, pageable);
        return hydrate(page);
    }

    private Page<FeedItemDto> hydrate(Page<FeedItem> page) {
        var userIds = page.getContent().stream().map(FeedItem::getUserId).distinct().toList();
        Map<Long, UserCache> byId = users.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserCache::getUserId, Function.identity()));
        return page.map(f -> {
            UserCache u = byId.get(f.getUserId());
            return FeedItemDto.from(f,
                    u != null ? u.getDisplayName() : "User #" + f.getUserId(),
                    u != null ? u.getAvatar() : null);
        });
    }
}
