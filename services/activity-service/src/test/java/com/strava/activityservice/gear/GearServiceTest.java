package com.strava.activityservice.gear;

import com.strava.activityservice.domain.ActivityType;
import com.strava.activityservice.domain.Gear;
import com.strava.activityservice.domain.GearRepository;
import com.strava.activityservice.domain.GearType;
import com.strava.activityservice.exception.ApiRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static com.strava.activityservice.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Ported from main's GearServiceImplTest. The user relation became a logical
 * userId, so the userService collaborator is gone; everything else mirrors main.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GearService Unit Tests")
class GearServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private GearRepository gearRepository;

    @InjectMocks
    private GearService gearService;

    private Gear shoes(Long id, Long userId) {
        return Gear.builder()
                .id(id)
                .userId(userId)
                .name("Saucony Endorphin Pro 3")
                .brand("Saucony")
                .model("Endorphin Pro 3")
                .type(GearType.SHOES)
                .activityTypes(EnumSet.of(ActivityType.RUN, ActivityType.VIRTUAL_RUN))
                .distanceKmTotal(0.0)
                .activityCount(0)
                .retired(false)
                .build();
    }

    private GearRequest shoesRequest() {
        GearRequest request = new GearRequest();
        request.setName("Saucony Endorphin Pro 3");
        request.setBrand("Saucony");
        request.setModel("Endorphin Pro 3");
        request.setType(GearType.SHOES);
        request.setActivityTypes(EnumSet.of(ActivityType.RUN));
        request.setNotes("Race day shoes");
        return request;
    }

    @Nested
    @DisplayName("createGear Tests")
    class CreateGearTests {

        @Test
        @DisplayName("Should create gear with provided fields and fresh counters")
        void shouldCreateGear() {
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> {
                Gear g = inv.getArgument(0);
                g.setId(10L);
                return g;
            });

            Gear result = gearService.createGear(OWNER_ID, shoesRequest());

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("Saucony Endorphin Pro 3");
            assertThat(result.getType()).isEqualTo(GearType.SHOES);
            assertThat(result.getUserId()).isEqualTo(OWNER_ID);
            assertThat(result.getDistanceKmTotal()).isZero();
            assertThat(result.getActivityCount()).isZero();
            assertThat(result.isRetired()).isFalse();
            assertThat(result.getActivityTypes()).containsExactly(ActivityType.RUN);
            verify(gearRepository).save(any(Gear.class));
        }

        @Test
        @DisplayName("Should default SHOES activity types when none provided")
        void shouldDefaultShoesActivityTypes() {
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = shoesRequest();
            req.setActivityTypes(EnumSet.noneOf(ActivityType.class));

            Gear result = gearService.createGear(OWNER_ID, req);

            assertThat(result.getActivityTypes())
                    .contains(ActivityType.RUN, ActivityType.VIRTUAL_RUN);
        }

        @Test
        @DisplayName("Should default BIKE activity types when none provided")
        void shouldDefaultBikeActivityTypes() {
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = new GearRequest();
            req.setName("Canyon Ultimate");
            req.setType(GearType.BIKE);
            req.setActivityTypes(null);

            Gear result = gearService.createGear(OWNER_ID, req);

            assertThat(result.getActivityTypes())
                    .containsExactlyInAnyOrder(ActivityType.RIDE, ActivityType.VIRTUAL_RIDE);
        }
    }

    @Nested
    @DisplayName("updateGear Authorization Tests")
    class UpdateAuthzTests {

        @Test
        @DisplayName("Should 403 when updating someone else's gear")
        void shouldReturn403WhenForeignUser() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.updateGear(10L, OTHER_USER_ID, shoesRequest()))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ACCESS_DENIED)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(gearRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should 404 when gear id does not exist")
        void shouldReturn404WhenMissing() {
            when(gearRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gearService.updateGear(999L, OWNER_ID, shoesRequest()))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_NOT_FOUND)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should update fields when caller is owner")
        void shouldUpdateWhenOwner() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = shoesRequest();
            req.setName("Saucony Endorphin Pro 4");
            req.setNotes("Updated notes");

            Gear result = gearService.updateGear(10L, OWNER_ID, req);

            assertThat(result.getName()).isEqualTo("Saucony Endorphin Pro 4");
            assertThat(result.getNotes()).isEqualTo("Updated notes");
            verify(gearRepository).save(gear);
        }
    }

    @Nested
    @DisplayName("retireGear Tests")
    class RetireTests {

        @Test
        @DisplayName("Should mark gear as retired and set retiredAt")
        void shouldRetireGear() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            Gear result = gearService.retireGear(10L, OWNER_ID);

            assertThat(result.isRetired()).isTrue();
            assertThat(result.getRetiredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should 400 when retiring already-retired gear")
        void shouldRejectDoubleRetire() {
            Gear gear = shoes(10L, OWNER_ID);
            gear.setRetired(true);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.retireGear(10L, OWNER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ALREADY_RETIRED)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should 403 when retiring someone else's gear")
        void shouldForbidRetireByNonOwner() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.retireGear(10L, OTHER_USER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("deleteGear Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete when owner")
        void shouldDeleteWhenOwner() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            gearService.deleteGear(10L, OWNER_ID);

            verify(gearRepository).delete(gear);
        }

        @Test
        @DisplayName("Should 403 when non-owner")
        void shouldForbidDeleteByNonOwner() {
            Gear gear = shoes(10L, OWNER_ID);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.deleteGear(10L, OTHER_USER_ID))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ACCESS_DENIED);

            verify(gearRepository, never()).delete(any(Gear.class));
        }
    }

    @Nested
    @DisplayName("getUserGear Tests")
    class GetUserGearTests {

        @Test
        @DisplayName("Should return all gear when includeRetired=true")
        void shouldReturnAll() {
            Gear a = shoes(1L, OWNER_ID);
            Gear b = shoes(2L, OWNER_ID);
            b.setRetired(true);
            when(gearRepository.findByUserIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(a, b));

            List<Gear> result = gearService.getUserGear(OWNER_ID, true);

            assertThat(result).hasSize(2);
            verify(gearRepository).findByUserIdOrderByCreatedAtDesc(OWNER_ID);
            verify(gearRepository, never()).findByUserIdAndRetiredFalseOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("Should return only active gear when includeRetired=false")
        void shouldReturnOnlyActive() {
            Gear a = shoes(1L, OWNER_ID);
            when(gearRepository.findByUserIdAndRetiredFalseOrderByCreatedAtDesc(OWNER_ID))
                    .thenReturn(List.of(a));

            List<Gear> result = gearService.getUserGear(OWNER_ID, false);

            assertThat(result).hasSize(1);
            verify(gearRepository).findByUserIdAndRetiredFalseOrderByCreatedAtDesc(OWNER_ID);
            verify(gearRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("Mileage bookkeeping Tests")
    class MileageTests {

        @Test
        @DisplayName("incrementGearMileage should add distance and bump activityCount")
        void shouldIncrementMileage() {
            Gear gear = shoes(10L, OWNER_ID);
            gear.setDistanceKmTotal(100.0);
            gear.setActivityCount(5);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            gearService.incrementGearMileage(10L, 7.5);

            ArgumentCaptor<Gear> captor = ArgumentCaptor.forClass(Gear.class);
            verify(gearRepository).save(captor.capture());
            assertThat(captor.getValue().getDistanceKmTotal()).isEqualTo(107.5);
            assertThat(captor.getValue().getActivityCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("incrementGearMileage should no-op when gearId null")
        void shouldNoopWhenGearIdNullOnIncrement() {
            gearService.incrementGearMileage(null, 10.0);
            verifyNoInteractions(gearRepository);
        }

        @Test
        @DisplayName("incrementGearMileage should no-op when distance <= 0")
        void shouldNoopWhenDistanceZeroOnIncrement() {
            gearService.incrementGearMileage(10L, 0.0);
            verifyNoInteractions(gearRepository);
        }

        @Test
        @DisplayName("decrementGearMileage should subtract distance and decrement activityCount")
        void shouldDecrementMileage() {
            Gear gear = shoes(10L, OWNER_ID);
            gear.setDistanceKmTotal(100.0);
            gear.setActivityCount(5);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            gearService.decrementGearMileage(10L, 7.5);

            ArgumentCaptor<Gear> captor = ArgumentCaptor.forClass(Gear.class);
            verify(gearRepository).save(captor.capture());
            assertThat(captor.getValue().getDistanceKmTotal()).isEqualTo(92.5);
            assertThat(captor.getValue().getActivityCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("decrementGearMileage should clamp at zero instead of going negative")
        void shouldClampDecrementAtZero() {
            Gear gear = shoes(10L, OWNER_ID);
            gear.setDistanceKmTotal(5.0);
            gear.setActivityCount(1);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            gearService.decrementGearMileage(10L, 100.0);

            ArgumentCaptor<Gear> captor = ArgumentCaptor.forClass(Gear.class);
            verify(gearRepository).save(captor.capture());
            assertThat(captor.getValue().getDistanceKmTotal()).isZero();
            assertThat(captor.getValue().getActivityCount()).isZero();
        }
    }
}
