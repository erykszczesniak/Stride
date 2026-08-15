package com.strava.userservice.event;

import com.strava.shared.events.Topics;
import com.strava.shared.events.UserFollowedEvent;
import com.strava.shared.events.UserUpdatedEvent;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventPublisher")
class UserEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafka;

    @InjectMocks
    private UserEventPublisher publisher;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).firstName("John").lastName("Doe").email("john@example.com")
                .avatar("http://avatar/1")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    @Test
    void publishesUserRegisteredWithDisplayNameAndAvatar() {
        given(kafka.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());

        publisher.publishUserRegistered(user);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq(Topics.USER_REGISTERED), eq("1"), payload.capture());
        var event = (com.strava.shared.events.UserRegisteredEvent) payload.getValue();
        assertThat(event.displayName()).isEqualTo("John Doe");
        assertThat(event.avatar()).isEqualTo("http://avatar/1");
    }

    @Test
    void publishesUserUpdatedWithFreshDisplayName() {
        given(kafka.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());
        user.setFirstName("Johnny");

        publisher.publishUserUpdated(user);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq(Topics.USER_UPDATED), eq("1"), payload.capture());
        assertThat(((UserUpdatedEvent) payload.getValue()).displayName()).isEqualTo("Johnny Doe");
    }

    @Test
    void publishesUserDeletedKeyedByUserId() {
        given(kafka.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());

        publisher.publishUserDeleted(1L);

        verify(kafka).send(eq(Topics.USER_DELETED), eq("1"), any());
    }

    @Test
    void publishesUserFollowedWithFollowerDisplayNameKeyedByFollowee() {
        given(kafka.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());

        publisher.publishUserFollowed(user, 2L);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafka).send(eq(Topics.USER_FOLLOWED), eq("2"), payload.capture());
        UserFollowedEvent event = (UserFollowedEvent) payload.getValue();
        assertThat(event.followerUserId()).isEqualTo(1L);
        assertThat(event.followeeUserId()).isEqualTo(2L);
        assertThat(event.followerDisplayName()).isEqualTo("John Doe");
    }

    @Test
    void publishesUserUnfollowed() {
        given(kafka.send(anyString(), anyString(), any())).willReturn(new CompletableFuture<>());

        publisher.publishUserUnfollowed(1L, 2L);

        verify(kafka).send(eq(Topics.USER_UNFOLLOWED), eq("2"), any());
    }

    @Test
    void asyncBrokerFailureIsSwallowed() {
        CompletableFuture<SendResult<String, Object>> failed =
                CompletableFuture.failedFuture(new RuntimeException("broker down"));
        given(kafka.send(anyString(), anyString(), any())).willReturn(failed);

        assertThatCode(() -> publisher.publishUserDeleted(1L)).doesNotThrowAnyException();
    }

    @Test
    void synchronousBrokerFailureIsSwallowed() {
        given(kafka.send(anyString(), anyString(), any()))
                .willThrow(new RuntimeException("no broker"));

        assertThatCode(() -> publisher.publishUserRegistered(user)).doesNotThrowAnyException();
    }
}
