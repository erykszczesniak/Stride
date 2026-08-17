package com.strava.socialservice.grpc;

import com.strava.grpc.social.GetSocialSummariesRequest;
import com.strava.grpc.social.GetSocialSummariesResponse;
import com.strava.grpc.social.SocialSummary;
import com.strava.socialservice.domain.ActivityLikeRepository;
import com.strava.socialservice.domain.CommentRepository;
import com.strava.socialservice.internal.SocialSummaryService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Direct-impl unit tests (no gRPC server, no Spring context): the service is
 * invoked with a capturing StreamObserver and backed by the real
 * {@link SocialSummaryService} over mocked repositories — the same fixtures as
 * the REST slice test, so both transports are verified against one contract.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SocialInternalGrpcService")
class SocialInternalGrpcServiceTest {

    @Mock private ActivityLikeRepository likeRepository;
    @Mock private CommentRepository commentRepository;

    private SocialInternalGrpcService grpcService;

    @BeforeEach
    void setUp() {
        grpcService = new SocialInternalGrpcService(new SocialSummaryService(likeRepository, commentRepository));
    }

    private static final class CapturingObserver implements StreamObserver<GetSocialSummariesResponse> {
        private final List<GetSocialSummariesResponse> values = new ArrayList<>();
        private Throwable error;
        private boolean completed;

        @Override public void onNext(GetSocialSummariesResponse value) { values.add(value); }
        @Override public void onError(Throwable t) { error = t; }
        @Override public void onCompleted() { completed = true; }

        GetSocialSummariesResponse single() {
            assertThat(error).isNull();
            assertThat(completed).isTrue();
            assertThat(values).hasSize(1);
            return values.get(0);
        }
    }

    @Test
    @DisplayName("returns counts and likedByViewer, zero-filling ids without social state")
    void countsAndZeroFilledRows() {
        when(likeRepository.countByActivityIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(commentRepository.countByActivityIdIn(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        when(likeRepository.findActivityIdsLikedBy(eq(9L), anyCollection()))
                .thenReturn(List.of(1L));

        CapturingObserver observer = new CapturingObserver();
        grpcService.getSocialSummaries(GetSocialSummariesRequest.newBuilder()
                .addActivityIds(1L).addActivityIds(2L)
                .setViewerId(9L)
                .build(), observer);

        List<SocialSummary> summaries = observer.single().getSummariesList();
        assertThat(summaries).hasSize(2);

        SocialSummary first = summaries.get(0);
        assertThat(first.getActivityId()).isEqualTo(1L);
        assertThat(first.getLikesCount()).isEqualTo(3);
        assertThat(first.getCommentsCount()).isEqualTo(2);
        assertThat(first.getLikedByViewer()).isTrue();

        SocialSummary second = summaries.get(1);
        assertThat(second.getActivityId()).isEqualTo(2L);
        assertThat(second.getLikesCount()).isZero();
        assertThat(second.getCommentsCount()).isZero();
        assertThat(second.getLikedByViewer()).isFalse();
    }

    @Test
    @DisplayName("without viewer_id no likedByViewer lookup happens and everything is false")
    void withoutViewer() {
        when(likeRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        when(commentRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());

        CapturingObserver observer = new CapturingObserver();
        grpcService.getSocialSummaries(GetSocialSummariesRequest.newBuilder()
                .addActivityIds(5L)
                .build(), observer);

        List<SocialSummary> summaries = observer.single().getSummariesList();
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getActivityId()).isEqualTo(5L);
        assertThat(summaries.get(0).getLikedByViewer()).isFalse();

        verify(likeRepository, never()).findActivityIdsLikedBy(anyLong(), anyCollection());
    }

    @Test
    @DisplayName("empty id list completes with an empty response without touching the repositories")
    void emptyIds() {
        CapturingObserver observer = new CapturingObserver();
        grpcService.getSocialSummaries(GetSocialSummariesRequest.getDefaultInstance(), observer);

        assertThat(observer.single().getSummariesCount()).isZero();
        verify(likeRepository, never()).countByActivityIdIn(anyCollection());
        verify(commentRepository, never()).countByActivityIdIn(anyCollection());
    }

    @Test
    @DisplayName("duplicate ids collapse to a single row, preserving request order")
    void duplicateIdsCollapse() {
        when(likeRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());
        when(commentRepository.countByActivityIdIn(anyCollection())).thenReturn(List.of());

        CapturingObserver observer = new CapturingObserver();
        grpcService.getSocialSummaries(GetSocialSummariesRequest.newBuilder()
                .addActivityIds(7L).addActivityIds(4L).addActivityIds(7L)
                .build(), observer);

        assertThat(observer.single().getSummariesList())
                .extracting(SocialSummary::getActivityId)
                .containsExactly(7L, 4L);
    }

    @Test
    @DisplayName("unexpected errors surface as Status.INTERNAL")
    void unexpectedErrorMapsToInternal() {
        when(likeRepository.countByActivityIdIn(anyCollection()))
                .thenThrow(new IllegalStateException("db down"));

        CapturingObserver observer = new CapturingObserver();
        grpcService.getSocialSummaries(GetSocialSummariesRequest.newBuilder()
                .addActivityIds(1L)
                .build(), observer);

        assertThat(observer.values).isEmpty();
        assertThat(observer.completed).isFalse();
        assertThat(observer.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(Status.fromThrowable(observer.error).getCode()).isEqualTo(Status.Code.INTERNAL);
    }
}
