package com.strava.clubevent.club;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.clubevent.client.UserBatchClient;
import com.strava.clubevent.client.UserSummary;
import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import com.strava.shared.security.HeaderAuthenticationFilter.HeaderAuthenticationToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClubController Unit Tests")
class ClubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubService clubService;

    @MockitoBean
    private UserBatchClient userBatchClient;

    private ObjectMapper objectMapper;
    private Club testClub;
    private ClubRequest clubRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testClub = Club.builder()
                .id(1L)
                .name("Running Club")
                .description("A club for runners")
                .city("Warsaw")
                .country("Poland")
                .sportType(ActivityType.RUN)
                .isPrivate(false)
                .ownerId(1L)
                .build();

        clubRequest = new ClubRequest();
        clubRequest.setName("Running Club");
        clubRequest.setDescription("A club for runners");
        clubRequest.setCity("Warsaw");
        clubRequest.setCountry("Poland");
        clubRequest.setSportType(ActivityType.RUN);
        clubRequest.setPrivate(false);

        given(userBatchClient.usersByIds(anyCollection())).willReturn(Map.of(
                1L, new UserSummary(1L, "John", "Doe", "John Doe", null)));

        SecurityContextHolder.getContext().setAuthentication(new HeaderAuthenticationToken(
                1L, "test@example.com", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/clubs")
    class GetAllClubsTests {

        @Test
        @DisplayName("Should return all public clubs")
        void shouldReturnAllPublicClubs() throws Exception {
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubService.getAllPublicClubs(any(Pageable.class))).willReturn(clubPage);
            given(clubService.isUserMember(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/clubs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Running Club"))
                    .andExpect(jsonPath("$[0].owner.firstName").value("John"))
                    .andExpect(jsonPath("$[0].owner.fullName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return empty list when no clubs")
        void shouldReturnEmptyListWhenNoClubs() throws Exception {
            Page<Club> emptyPage = new PageImpl<>(Collections.emptyList());
            given(clubService.getAllPublicClubs(any(Pageable.class))).willReturn(emptyPage);

            mockMvc.perform(get("/api/v1/clubs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("Should filter clubs by sport type")
        void shouldFilterClubsBySportType() throws Exception {
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubService.getClubsBySportType(eq(ActivityType.RUN), any(Pageable.class))).willReturn(clubPage);
            given(clubService.isUserMember(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/clubs")
                            .param("sportType", "RUN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].sportType").value("RUN"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/stats")
    class GetClubStatsTests {

        @Test
        @DisplayName("Should return club stats")
        void shouldReturnClubStats() throws Exception {
            given(clubService.countPublicClubs()).willReturn(10L);
            given(clubService.getClubsByUserId(1L)).willReturn(List.of(testClub, testClub));
            given(clubService.countTotalMembers()).willReturn(100L);

            mockMvc.perform(get("/api/v1/clubs/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalClubs").value(10))
                    .andExpect(jsonPath("$.userClubs").value(2))
                    .andExpect(jsonPath("$.totalMembers").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/{clubId}")
    class GetClubByIdTests {

        @Test
        @DisplayName("Should return club when found")
        void shouldReturnClubWhenFound() throws Exception {
            given(clubService.getClubById(1L)).willReturn(testClub);
            given(clubService.isUserMember(1L, 1L)).willReturn(true);

            mockMvc.perform(get("/api/v1/clubs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Running Club"))
                    .andExpect(jsonPath("$.member").value(true));
        }

        @Test
        @DisplayName("Should return 404 with message property when club not found")
        void shouldReturn404WhenClubNotFound() throws Exception {
            given(clubService.getClubById(99L))
                    .willThrow(new NotFoundException("Club not found"));

            mockMvc.perform(get("/api/v1/clubs/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Club not found"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/my")
    class GetMyClubsTests {

        @Test
        @DisplayName("Should return user's clubs")
        void shouldReturnUserClubs() throws Exception {
            given(clubService.getClubsByUserId(1L)).willReturn(List.of(testClub));
            given(clubService.isUserMember(1L, 1L)).willReturn(true);

            mockMvc.perform(get("/api/v1/clubs/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/clubs")
    class CreateClubTests {

        @Test
        @DisplayName("Should create club successfully")
        void shouldCreateClubSuccessfully() throws Exception {
            given(clubService.createClub(any(Club.class), eq(1L))).willReturn(testClub);
            given(clubService.isUserMember(1L, 1L)).willReturn(true);

            mockMvc.perform(post("/api/v1/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clubRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Running Club"));
        }

        @Test
        @DisplayName("Should return bad request when name is blank")
        void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
            clubRequest.setName("");

            mockMvc.perform(post("/api/v1/clubs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clubRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/clubs/{clubId}")
    class UpdateClubTests {

        @Test
        @DisplayName("Should update club successfully")
        void shouldUpdateClubSuccessfully() throws Exception {
            testClub.setName("Updated Club Name");
            given(clubService.updateClub(eq(1L), any(Club.class), eq(1L))).willReturn(testClub);
            given(clubService.isUserMember(1L, 1L)).willReturn(true);

            mockMvc.perform(put("/api/v1/clubs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clubRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Club Name"));
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            given(clubService.updateClub(eq(1L), any(Club.class), eq(1L)))
                    .willThrow(new ForbiddenException("Not authorized"));

            mockMvc.perform(put("/api/v1/clubs/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clubRequest)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/clubs/{clubId}")
    class DeleteClubTests {

        @Test
        @DisplayName("Should delete club successfully")
        void shouldDeleteClubSuccessfully() throws Exception {
            willDoNothing().given(clubService).deleteClub(1L, 1L);

            mockMvc.perform(delete("/api/v1/clubs/1"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/clubs/{clubId}/join")
    class JoinClubTests {

        @Test
        @DisplayName("Should join club successfully")
        void shouldJoinClubSuccessfully() throws Exception {
            willDoNothing().given(clubService).joinClub(1L, 1L);

            mockMvc.perform(post("/api/v1/clubs/1/join"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully joined the club"));
        }

        @Test
        @DisplayName("Should return error when already member")
        void shouldReturnErrorWhenAlreadyMember() throws Exception {
            willThrow(new BadRequestException("Already a member"))
                    .given(clubService).joinClub(1L, 1L);

            mockMvc.perform(post("/api/v1/clubs/1/join"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/clubs/{clubId}/leave")
    class LeaveClubTests {

        @Test
        @DisplayName("Should leave club successfully")
        void shouldLeaveClubSuccessfully() throws Exception {
            willDoNothing().given(clubService).leaveClub(1L, 1L);

            mockMvc.perform(post("/api/v1/clubs/1/leave"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully left the club"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/{clubId}/members")
    class GetClubMembersTests {

        @Test
        @DisplayName("Should return club members")
        void shouldReturnClubMembers() throws Exception {
            given(clubService.getClubMemberIds(1L)).willReturn(List.of(1L));

            mockMvc.perform(get("/api/v1/clubs/1/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"))
                    .andExpect(jsonPath("$[0].fullName").value("John Doe"));
        }

        @Test
        @DisplayName("Should render placeholder members when user lookup falls back")
        void shouldRenderPlaceholderMembersWhenUserLookupFallsBack() throws Exception {
            given(clubService.getClubMemberIds(1L)).willReturn(List.of(7L));
            given(userBatchClient.usersByIds(anyCollection())).willReturn(Map.of());

            mockMvc.perform(get("/api/v1/clubs/1/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(7))
                    .andExpect(jsonPath("$[0].fullName").value("User #7"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/search")
    class SearchClubsTests {

        @Test
        @DisplayName("Should search clubs by query")
        void shouldSearchClubsByQuery() throws Exception {
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubService.searchClubs(eq("Running"), any(Pageable.class))).willReturn(clubPage);
            given(clubService.isUserMember(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/clubs/search")
                            .param("q", "Running"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Running Club"));
        }
    }
}
