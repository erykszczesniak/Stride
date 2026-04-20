package com.strava.backend.service.impl;

import com.strava.backend.domain.Gear;
import com.strava.backend.domain.User;
import com.strava.backend.dto.gear.GearRequest;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.enums.GearType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.GearRepository;
import com.strava.backend.service.UserService;
import com.strava.backend.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
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

import static com.strava.backend.constants.ErrorMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GearServiceImpl Unit Tests")
class GearServiceImplTest {

    @Mock
    private GearRepository gearRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GearServiceImpl gearService;

    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        owner = TestDataFactory.createUser(1L, "owner@example.com", "Owner", "User");
        otherUser = TestDataFactory.createUser(2L, "other@example.com", "Other", "User");
    }

    private Gear shoes(Long id, User user) {
        return Gear.builder()
                .id(id)
                .user(user)
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
            when(userService.getUserById(1L)).thenReturn(owner);
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> {
                Gear g = inv.getArgument(0);
                g.setId(10L);
                return g;
            });

            Gear result = gearService.createGear(1L, shoesRequest());

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getName()).isEqualTo("Saucony Endorphin Pro 3");
            assertThat(result.getType()).isEqualTo(GearType.SHOES);
            assertThat(result.getUser()).isEqualTo(owner);
            assertThat(result.getDistanceKmTotal()).isZero();
            assertThat(result.getActivityCount()).isZero();
            assertThat(result.isRetired()).isFalse();
            assertThat(result.getActivityTypes()).containsExactly(ActivityType.RUN);
            verify(gearRepository).save(any(Gear.class));
        }

        @Test
        @DisplayName("Should default SHOES activity types when none provided")
        void shouldDefaultShoesActivityTypes() {
            when(userService.getUserById(1L)).thenReturn(owner);
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = shoesRequest();
            req.setActivityTypes(EnumSet.noneOf(ActivityType.class));

            Gear result = gearService.createGear(1L, req);

            assertThat(result.getActivityTypes())
                    .contains(ActivityType.RUN, ActivityType.VIRTUAL_RUN);
        }

        @Test
        @DisplayName("Should default BIKE activity types when none provided")
        void shouldDefaultBikeActivityTypes() {
            when(userService.getUserById(1L)).thenReturn(owner);
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = new GearRequest();
            req.setName("Canyon Ultimate");
            req.setType(GearType.BIKE);
            req.setActivityTypes(null);

            Gear result = gearService.createGear(1L, req);

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
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.updateGear(10L, otherUser.getId(), shoesRequest()))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ACCESS_DENIED)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

            verify(gearRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should 404 when gear id does not exist")
        void shouldReturn404WhenMissing() {
            when(gearRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gearService.updateGear(999L, owner.getId(), shoesRequest()))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_NOT_FOUND)
                    .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should update fields when caller is owner")
        void shouldUpdateWhenOwner() {
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            GearRequest req = shoesRequest();
            req.setName("Saucony Endorphin Pro 4");
            req.setNotes("Updated notes");

            Gear result = gearService.updateGear(10L, owner.getId(), req);

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
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));
            when(gearRepository.save(any(Gear.class))).thenAnswer(inv -> inv.getArgument(0));

            Gear result = gearService.retireGear(10L, owner.getId());

            assertThat(result.isRetired()).isTrue();
            assertThat(result.getRetiredAt()).isNotNull();
        }

        @Test
        @DisplayName("Should 400 when retiring already-retired gear")
        void shouldRejectDoubleRetire() {
            Gear gear = shoes(10L, owner);
            gear.setRetired(true);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.retireGear(10L, owner.getId()))
                    .isInstanceOf(ApiRequestException.class)
                    .hasMessage(GEAR_ALREADY_RETIRED)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should 403 when retiring someone else's gear")
        void shouldForbidRetireByNonOwner() {
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.retireGear(10L, otherUser.getId()))
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
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            gearService.deleteGear(10L, owner.getId());

            verify(gearRepository).delete(gear);
        }

        @Test
        @DisplayName("Should 403 when non-owner")
        void shouldForbidDeleteByNonOwner() {
            Gear gear = shoes(10L, owner);
            when(gearRepository.findById(10L)).thenReturn(Optional.of(gear));

            assertThatThrownBy(() -> gearService.deleteGear(10L, otherUser.getId()))
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
            Gear a = shoes(1L, owner);
            Gear b = shoes(2L, owner);
            b.setRetired(true);
            when(gearRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(a, b));

            List<Gear> result = gearService.getUserGear(1L, true);

            assertThat(result).hasSize(2);
            verify(gearRepository).findByUserIdOrderByCreatedAtDesc(1L);
            verify(gearRepository, never()).findByUserIdAndRetiredFalseOrderByCreatedAtDesc(any());
        }

        @Test
        @DisplayName("Should return only active gear when includeRetired=false")
        void shouldReturnOnlyActive() {
            Gear a = shoes(1L, owner);
            when(gearRepository.findByUserIdAndRetiredFalseOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(a));

            List<Gear> result = gearService.getUserGear(1L, false);

            assertThat(result).hasSize(1);
            verify(gearRepository).findByUserIdAndRetiredFalseOrderByCreatedAtDesc(1L);
            verify(gearRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
        }
    }

    @Nested
    @DisplayName("Mileage bookkeeping Tests")
    class MileageTests {

        @Test
        @DisplayName("incrementGearMileage should add distance and bump activityCount")
        void shouldIncrementMileage() {
            Gear gear = shoes(10L, owner);
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
            Gear gear = shoes(10L, owner);
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
            Gear gear = shoes(10L, owner);
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
