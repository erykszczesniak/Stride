package com.strava.backend.search;

import com.strava.backend.domain.User;
import com.strava.backend.enums.AuthProvider;
import com.strava.backend.enums.Role;
import com.strava.backend.event.UserDeletedEvent;
import com.strava.backend.event.UserUpsertedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link UserSearchIndexSyncListener}: verifies the listener
 * translates events into the expected repository operations and swallows
 * downstream Elasticsearch errors so user saves are never blocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSearchIndexSyncListener")
class UserSearchIndexSyncListenerTest {

    @Mock
    private UserSearchRepository userSearchRepository;

    @InjectMocks
    private UserSearchIndexSyncListener listener;

    private User sampleUser() {
        return User.builder()
                .id(42L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@example.com")
                .bio("Mathematician")
                .city("London")
                .country("UK")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .hideFromSearch(false)
                .build();
    }

    @Test
    @DisplayName("upsert event writes UserDocument with mapped fields and computed fullName")
    void upsertEventCallsSaveWithMappedDocument() {
        listener.onUserUpserted(new UserUpsertedEvent(sampleUser()));

        ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
        verify(userSearchRepository).save(captor.capture());

        UserDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        assertThat(saved.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getBio()).isEqualTo("Mathematician");
        assertThat(saved.getHideFromSearch()).isFalse();
    }

    @Test
    @DisplayName("delete event calls deleteById on the search repo")
    void deleteEventCallsDeleteById() {
        listener.onUserDeleted(new UserDeletedEvent(7L));
        verify(userSearchRepository).deleteById(7L);
    }

    @Test
    @DisplayName("upsert swallows exceptions so DB writes are never blocked by ES failures")
    void upsertSwallowsRepositoryFailures() {
        doThrow(new RuntimeException("ES cluster unreachable"))
                .when(userSearchRepository).save(any(UserDocument.class));

        // Should not propagate.
        listener.onUserUpserted(new UserUpsertedEvent(sampleUser()));

        verify(userSearchRepository).save(any(UserDocument.class));
    }
}
