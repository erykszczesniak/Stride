package com.strava.userservice.search;

import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/** Ported from the monolith's {@code UserSearchIndexSyncListenerTest}. */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserSearchIndexSyncListener")
class UserSearchIndexSyncListenerTest {

    @Mock
    private UserSearchRepository userSearchRepository;

    @InjectMocks
    private UserSearchIndexSyncListener listener;

    private User user() {
        return User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john@example.com")
                .bio("bio").city("Warsaw").country("Poland")
                .hideFromSearch(true)
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    @Test
    void upsertIndexesMappedDocument() {
        listener.onUserUpserted(new UserUpsertedEvent(user()));

        ArgumentCaptor<UserDocument> doc = ArgumentCaptor.forClass(UserDocument.class);
        verify(userSearchRepository).save(doc.capture());
        assertThat(doc.getValue().getId()).isEqualTo(1L);
        assertThat(doc.getValue().getFullName()).isEqualTo("John Doe");
        assertThat(doc.getValue().getCity()).isEqualTo("Warsaw");
        assertThat(doc.getValue().getHideFromSearch()).isTrue();
    }

    @Test
    void upsertFailureIsSwallowed() {
        given(userSearchRepository.save(any())).willThrow(new RuntimeException("ES down"));

        assertThatCode(() -> listener.onUserUpserted(new UserUpsertedEvent(user())))
                .doesNotThrowAnyException();
    }

    @Test
    void deleteRemovesDocument() {
        listener.onUserDeleted(new UserDeletedEvent(1L));

        verify(userSearchRepository).deleteById(1L);
    }

    @Test
    void deleteFailureIsSwallowed() {
        willThrow(new RuntimeException("ES down")).given(userSearchRepository).deleteById(1L);

        assertThatCode(() -> listener.onUserDeleted(new UserDeletedEvent(1L)))
                .doesNotThrowAnyException();
    }
}
