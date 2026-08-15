package com.strava.activityservice.activity;

import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.client.UserVisibilityClient.VisibilityInfo;
import com.strava.activityservice.domain.Activity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityVisibility")
class ActivityVisibilityTest {

    @Mock
    private UserVisibilityClient userVisibilityClient;

    @InjectMocks
    private ActivityVisibility visibility;

    private Activity activity(long ownerId, boolean privateActivity) {
        Activity activity = Activity.builder().id(1L).userId(ownerId).build();
        activity.setPrivate(privateActivity);
        return activity;
    }

    @Test
    @DisplayName("owners always see their own activities, private or not")
    void ownerSeesOwn() {
        assertThat(visibility.isViewableBy(7L, activity(7, true))).isTrue();
        verifyNoInteractions(userVisibilityClient);
    }

    @Test
    @DisplayName("strangers see public activities of public profiles")
    void publicActivityPublicProfile() {
        given(userVisibilityClient.visibilityFor(9L, 7L)).willReturn(new VisibilityInfo(false, false));
        assertThat(visibility.isViewableBy(7L, activity(9, false))).isTrue();
    }

    @Test
    @DisplayName("private activities are hidden from everyone but the owner")
    void privateActivityHidden() {
        assertThat(visibility.isViewableBy(7L, activity(9, true))).isFalse();
        verifyNoInteractions(userVisibilityClient);
    }

    @Test
    @DisplayName("private profiles hide activities from strangers but not followers")
    void privateProfileNeedsFollow() {
        given(userVisibilityClient.visibilityFor(9L, 7L)).willReturn(new VisibilityInfo(true, false));
        assertThat(visibility.isViewableBy(7L, activity(9, false))).isFalse();

        given(userVisibilityClient.visibilityFor(9L, 7L)).willReturn(new VisibilityInfo(true, true));
        assertThat(visibility.isViewableBy(7L, activity(9, false))).isTrue();
    }

    @Test
    @DisplayName("ownerless or anonymous lookups are refused conservatively")
    void conservativeDefaults() {
        Activity orphan = Activity.builder().id(1L).build();
        assertThat(visibility.isViewableBy(7L, orphan)).isFalse();

        given(userVisibilityClient.visibilityFor(9L, null)).willReturn(new VisibilityInfo(true, false));
        assertThat(visibility.isViewableBy(null, activity(9, false))).isFalse();
    }

    @Test
    @DisplayName("user-service outage falls back to a not-private profile — public content stays up")
    void fallbackKeepsPublicContentVisible() {
        given(userVisibilityClient.visibilityFor(9L, 7L)).willReturn(VisibilityInfo.FALLBACK);
        assertThat(visibility.isViewableBy(7L, activity(9, false))).isTrue();
        // activity-level privacy is still enforced locally
        assertThat(visibility.isViewableBy(7L, activity(9, true))).isFalse();
    }
}
