package com.strava.activityservice.activity;

import com.strava.activityservice.client.SocialSummaryClient;
import com.strava.activityservice.client.SocialSummaryClient.SocialSummary;
import com.strava.activityservice.client.UserVisibilityClient;
import com.strava.activityservice.client.UserVisibilityClient.VisibilityInfo;
import com.strava.activityservice.common.HeaderResponse;
import com.strava.activityservice.domain.Activity;
import com.strava.activityservice.exception.ApiRequestException;
import com.strava.activityservice.user.BaseUserResponse;
import com.strava.activityservice.user.UserCacheService;
import com.strava.activityservice.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.strava.activityservice.constants.ErrorMessage.ACTIVITY_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityMapper")
class ActivityMapperTest {

    @Mock
    private ActivityVisibility activityVisibility;

    // The mapper delegates persistence to the real service — mock it at the
    // repository boundary instead of re-testing service logic here.
    @Mock
    private com.strava.activityservice.domain.ActivityRepository activityRepository;

    @Mock
    private UserVisibilityClient userVisibilityClient;

    @Mock
    private SocialSummaryClient socialSummaryClient;

    @Mock
    private UserCacheService userCacheService;

    private ActivityMapper mapper;

    private Activity activity;

    @BeforeEach
    void setUp() {
        ActivityService activityService = new ActivityService(activityRepository, null, null);
        mapper = new ActivityMapper(activityVisibility, activityService,
                userVisibilityClient, socialSummaryClient, userCacheService);

        activity = TestDataFactory.createActivity(1L, 5L);
        BaseUserResponse author = new BaseUserResponse();
        author.setId(5L);
        author.setFirstName("John");
        author.setLastName("Doe");
        lenient().when(userCacheService.userFor(5L)).thenReturn(author);
        lenient().when(userCacheService.usersFor(anyCollection())).thenReturn(Map.of(5L, author));
    }

    @Test
    @DisplayName("detail view hides denied activities behind the same 404 as missing ones")
    void detailDeniedIs404() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityVisibility.isViewableBy(9L, activity)).thenReturn(false);

        assertThatThrownBy(() -> mapper.getActivityById(1L, 9L))
                .isInstanceOf(ApiRequestException.class)
                .hasMessage(ACTIVITY_NOT_FOUND);
    }

    @Test
    @DisplayName("detail view composes social counts, author and parsed GPS streams")
    void detailComposesEverything() {
        activity.setElevationProfile("[10.0,null,12.0]");
        activity.setPointTimestamps("[100,160,220]");
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityVisibility.isViewableBy(9L, activity)).thenReturn(true);
        when(socialSummaryClient.summariesFor(List.of(1L), 9L))
                .thenReturn(Map.of(1L, new SocialSummary(1L, 7, 3, true)));

        FullActivityResponse response = mapper.getActivityById(1L, 9L);

        assertThat(response.getLikesCount()).isEqualTo(7);
        assertThat(response.getCommentsCount()).isEqualTo(3);
        assertThat(response.isLikedByCurrentUser()).isTrue();
        assertThat(response.getUser().getId()).isEqualTo(5L);
        assertThat(response.getUser().getFullName()).isEqualTo("John Doe");
        assertThat(response.getElevationProfile()).containsExactly(10.0, 0.0, 12.0);
        assertThat(response.getPointTimestamps()).containsExactly(100L, 160L, 220L);
        assertThat(response.getComments()).isEmpty();
    }

    @Test
    @DisplayName("social-service outage degrades counts to zeros instead of failing the page")
    void socialFallbackDegradesToZeros() {
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityVisibility.isViewableBy(9L, activity)).thenReturn(true);
        when(socialSummaryClient.summariesFor(List.of(1L), 9L)).thenReturn(Map.of());

        FullActivityResponse response = mapper.getActivityById(1L, 9L);

        assertThat(response.getLikesCount()).isZero();
        assertThat(response.getCommentsCount()).isZero();
        assertThat(response.isLikedByCurrentUser()).isFalse();
    }

    @Test
    @DisplayName("a private profile returns an empty page to strangers, without touching the DB")
    void privateProfileEmptyForStrangers() {
        when(userVisibilityClient.visibilityFor(5L, 9L)).thenReturn(new VisibilityInfo(true, false));

        Pageable pageable = PageRequest.of(0, 10);
        HeaderResponse<ActivityResponse> response = mapper.getActivitiesByUserId(5L, 9L, pageable);

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getHeaders().getFirst("page-total-count")).isEqualTo("0");
        assertThat(response.getHeaders().getFirst("page-total-elements")).isEqualTo("0");
        verify(activityRepository, never()).findByUserId(any(), any());
    }

    @Test
    @DisplayName("a follower of a private profile gets the normal page with pagination headers")
    void followerSeesPrivateProfilePage() {
        when(userVisibilityClient.visibilityFor(5L, 9L)).thenReturn(new VisibilityInfo(true, true));
        Pageable pageable = PageRequest.of(0, 10);
        when(activityRepository.findByUserId(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(activity), pageable, 1));
        when(socialSummaryClient.summariesFor(List.of(1L), 9L)).thenReturn(Map.of());

        HeaderResponse<ActivityResponse> response = mapper.getActivitiesByUserId(5L, 9L, pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getHeaders().getFirst("page-total-count")).isEqualTo("1");
        assertThat(response.getHeaders().getFirst("page-total-elements")).isEqualTo("1");
    }

    @Test
    @DisplayName("owners skip the remote profile-privacy check for their own list")
    void ownerSkipsPrivacyCheck() {
        Pageable pageable = PageRequest.of(0, 10);
        when(activityRepository.findByUserId(5L, pageable))
                .thenReturn(new PageImpl<>(List.of(activity), pageable, 1));
        when(socialSummaryClient.summariesFor(List.of(1L), 5L)).thenReturn(Map.of());

        mapper.getActivitiesByUserId(5L, 5L, pageable);

        verify(userVisibilityClient, never()).visibilityFor(any(), any());
    }
}
