package com.strava.userservice.user;

import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserFollowRepository;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final UserFollowRepository follows;

    @Transactional(readOnly = true)
    public UserProfileDto getById(Long id) {
        User u = users.findById(id).orElseThrow(() -> new NotFoundException("User " + id + " not found"));
        return UserProfileDto.from(u, follows.countByUserId(id), follows.countByFollowerId(id));
    }

    @Transactional(readOnly = true)
    public Page<UserProfileDto> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }
        return users.search(query.trim(), pageable)
                .map(u -> UserProfileDto.from(u,
                        follows.countByUserId(u.getId()),
                        follows.countByFollowerId(u.getId())));
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest req) {
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
        if (req.firstName() != null) u.setFirstName(req.firstName());
        if (req.lastName() != null) u.setLastName(req.lastName());
        if (req.avatar() != null) u.setAvatar(req.avatar());
        if (req.bio() != null) u.setBio(req.bio());
        if (req.city() != null) u.setCity(req.city());
        if (req.country() != null) u.setCountry(req.country());
        if (req.weight() != null) u.setWeight(req.weight());
        if (req.height() != null) u.setHeight(req.height());
        if (req.isPrivate() != null) u.setIsPrivate(req.isPrivate());
        if (req.hideFromSearch() != null) u.setHideFromSearch(req.hideFromSearch());
        return UserProfileDto.from(u, follows.countByUserId(userId), follows.countByFollowerId(userId));
    }
}
