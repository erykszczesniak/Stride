package com.strava.backend.service.impl;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.UserFeedEntry;
import com.strava.backend.repository.ActivityRepository;
import com.strava.backend.repository.UserFeedEntryRepository;
import com.strava.backend.service.ActivityFeedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ActivityFeedQueryServiceImpl implements ActivityFeedQueryService {

    private final UserFeedEntryRepository feedRepository;
    private final ActivityRepository activityRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getFeedForUser(Long userId, Pageable pageable) {
        Page<UserFeedEntry> page = feedRepository
                .findByRecipientUserIdOrderByActivityCreatedAtDescIdDesc(userId, pageable);
        if (page.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        // Hydrate Activity rows in one round-trip, preserving feed order.
        // findAllById returns in an arbitrary order, so reorder via a lookup map.
        List<Long> activityIds = page.getContent().stream()
                .map(UserFeedEntry::getActivityId)
                .toList();
        Map<Long, Activity> byId = new LinkedHashMap<>();
        for (Activity a : activityRepository.findAllById(activityIds)) {
            byId.put(a.getId(), a);
        }
        List<Activity> ordered = activityIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
        return new PageImpl<>(ordered, pageable, page.getTotalElements());
    }
}
