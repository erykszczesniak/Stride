package com.strava.backend.service;

import com.strava.backend.domain.Activity;
import com.strava.backend.domain.User;
import com.strava.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityVisibility")
class ActivityVisibilityTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ActivityVisibility visibility;

    private Activity activity(long ownerId, boolean privateActivity, boolean privateProfile) {
        User owner = User.builder().id(ownerId).isPrivate(privateProfile).build();
        Activity activity = Activity.builder().id(1L).user(owner).build();
        activity.setPrivate(privateActivity);
        return activity;
    }

    @Test
    @DisplayName("owners always see their own activities, private or not")
    void ownerSeesOwn() {
        assertThat(visibility.isViewableBy(7L, activity(7, true, true))).isTrue();
    }

    @Test
    @DisplayName("strangers see public activities of public profiles")
    void publicActivityPublicProfile() {
        assertThat(visibility.isViewableBy(7L, activity(9, false, false))).isTrue();
    }

    @Test
    @DisplayName("private activities are hidden from everyone but the owner")
    void privateActivityHidden() {
        assertThat(visibility.isViewableBy(7L, activity(9, true, false))).isFalse();
    }

    @Test
    @DisplayName("private profiles hide activities from strangers but not followers")
    void privateProfileNeedsFollow() {
        given(userRepository.isFollowing(9L, 7L)).willReturn(false);
        assertThat(visibility.isViewableBy(7L, activity(9, false, true))).isFalse();

        given(userRepository.isFollowing(9L, 7L)).willReturn(true);
        assertThat(visibility.isViewableBy(7L, activity(9, false, true))).isTrue();
    }

    @Test
    @DisplayName("ownerless or anonymous lookups are refused conservatively")
    void conservativeDefaults() {
        Activity orphan = Activity.builder().id(1L).build();
        assertThat(visibility.isViewableBy(7L, orphan)).isFalse();
        assertThat(visibility.isViewableBy(null, activity(9, false, true))).isFalse();
    }
}
