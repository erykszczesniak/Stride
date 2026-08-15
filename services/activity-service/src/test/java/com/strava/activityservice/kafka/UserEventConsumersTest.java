package com.strava.activityservice.kafka;

import com.strava.activityservice.domain.UserCache;
import com.strava.activityservice.domain.UserCacheRepository;
import com.strava.shared.events.UserDeletedEvent;
import com.strava.shared.events.UserRegisteredEvent;
import com.strava.shared.events.UserUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventConsumers")
class UserEventConsumersTest {

    @Mock
    private UserCacheRepository users;

    @InjectMocks
    private UserEventConsumers consumers;

    @Test
    @DisplayName("user.registered upserts a cache row with a split display name")
    void registeredUpserts() {
        consumers.onUserRegistered(new UserRegisteredEvent(
                7L, "john@example.com", "John Doe", "/avatars/j.png", Instant.now()));

        ArgumentCaptor<UserCache> captor = ArgumentCaptor.forClass(UserCache.class);
        verify(users).save(captor.capture());
        UserCache cached = captor.getValue();
        assertThat(cached.getUserId()).isEqualTo(7L);
        assertThat(cached.getFirstName()).isEqualTo("John");
        assertThat(cached.getLastName()).isEqualTo("Doe");
        assertThat(cached.getAvatar()).isEqualTo("/avatars/j.png");
    }

    @Test
    @DisplayName("user.updated refreshes the same row (idempotent upsert)")
    void updatedRefreshes() {
        consumers.onUserUpdated(new UserUpdatedEvent(7L, "Johnny B Goode", null, Instant.now()));

        ArgumentCaptor<UserCache> captor = ArgumentCaptor.forClass(UserCache.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Johnny");
        assertThat(captor.getValue().getLastName()).isEqualTo("B Goode");
    }

    @Test
    @DisplayName("user.deleted evicts the row")
    void deletedEvicts() {
        consumers.onUserDeleted(new UserDeletedEvent(7L, Instant.now()));

        verify(users).deleteById(7L);
    }

    @Test
    @DisplayName("display-name splitting handles single-word and blank names")
    void displayNameSplitting() {
        assertThat(UserEventConsumers.splitDisplayName("Cher")).containsExactly("Cher", null);
        assertThat(UserEventConsumers.splitDisplayName("  ")).containsExactly(null, null);
        assertThat(UserEventConsumers.splitDisplayName(null)).containsExactly(null, null);
        assertThat(UserEventConsumers.splitDisplayName(" Jane  Smith ")).containsExactly("Jane", "Smith");
    }
}
