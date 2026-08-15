package com.strava.userservice.admin;

import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import com.strava.userservice.event.UserEventPublisher;
import com.strava.userservice.exception.ApiRequestException;
import com.strava.userservice.search.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.strava.userservice.common.ErrorMessages.USER_NOT_FOUND;

/** User-side admin operations, mirrored from the monolith's {@code AdminServiceImpl}. */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher localEvents;
    private final UserEventPublisher kafkaEvents;

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    @Transactional
    public void updateUserRole(Long userId, Role role) {
        User user = getUserById(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        User user = getUserById(userId);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
        // The monolith skipped events here (leaving a stale ES doc); in a distributed
        // deployment other services must learn about the deletion, so we publish the
        // same events as a self-service account deletion. Public contract is unchanged.
        localEvents.publishEvent(new UserDeletedEvent(userId));
        kafkaEvents.publishUserDeleted(userId);
    }
}
