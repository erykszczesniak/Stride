package com.strava.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.dto.club.ClubRequest;
import com.strava.backend.dto.user.BaseUserResponse;
import com.strava.backend.enums.ActivityType;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.mapper.CommonMapper;
import com.strava.backend.security.CustomUserDetailsService;
import com.strava.backend.security.jwt.JwtProvider;
import com.strava.backend.service.ClubService;
import com.strava.backend.util.TestDataFactory;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClubController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("ClubController Unit Tests")
class ClubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClubService clubService;

    @MockitoBean
    private CommonMapper commonMapper;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private ObjectMapper objectMapper;
    private Club testClub;
    private User testUser;
    private ClubRequest clubRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testUser = TestDataFactory.createUser();
        testClub = TestDataFactory.createClub(1L, testUser);

        clubRequest = new ClubRequest();
        clubRequest.setName("Running Club");
        clubRequest.setDescription("A club for runners");
        clubRequest.setCity("Warsaw");
        clubRequest.setCountry("Poland");
        clubRequest.setSportType(ActivityType.RUN);
        clubRequest.setPrivate(false);
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

            mockMvc.perform(get("/api/v1/clubs")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Running Club"));
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
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/{clubId}")
    class GetClubByIdTests {

        @Test
        @DisplayName("Should return club when found")
        void shouldReturnClubWhenFound() throws Exception {
            given(clubService.getClubById(1L)).willReturn(testClub);
            given(clubService.isUserMember(1L, 1L)).willReturn(true);

            mockMvc.perform(get("/api/v1/clubs/1")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Running Club"))
                    .andExpect(jsonPath("$.member").value(true));
        }

        @Test
        @DisplayName("Should return 404 when club not found")
        void shouldReturn404WhenClubNotFound() throws Exception {
            given(clubService.getClubById(99L))
                    .willThrow(new ApiRequestException("Club not found", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/v1/clubs/99"))
                    .andExpect(status().isNotFound());
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

            mockMvc.perform(get("/api/v1/clubs/my")
                            .param("userId", "1"))
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
                            .param("userId", "1")
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
                            .param("userId", "1")
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
                            .param("userId", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(clubRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Club Name"));
        }

        @Test
        @DisplayName("Should return 403 when not owner")
        void shouldReturn403WhenNotOwner() throws Exception {
            given(clubService.updateClub(eq(1L), any(Club.class), eq(2L)))
                    .willThrow(new ApiRequestException("Not authorized", HttpStatus.FORBIDDEN));

            mockMvc.perform(put("/api/v1/clubs/1")
                            .param("userId", "2")
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

            mockMvc.perform(delete("/api/v1/clubs/1")
                            .param("userId", "1"))
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

            mockMvc.perform(post("/api/v1/clubs/1/join")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully joined the club"));
        }

        @Test
        @DisplayName("Should return error when already member")
        void shouldReturnErrorWhenAlreadyMember() throws Exception {
            willThrow(new ApiRequestException("Already a member", HttpStatus.BAD_REQUEST))
                    .given(clubService).joinClub(1L, 1L);

            mockMvc.perform(post("/api/v1/clubs/1/join")
                            .param("userId", "1"))
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

            mockMvc.perform(post("/api/v1/clubs/1/leave")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Successfully left the club"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs/{clubId}/members")
    class GetClubMembersTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Should return club members")
        void shouldReturnClubMembers() throws Exception {
            given(clubService.getClubMembers(1L)).willAnswer(invocation -> List.of(testUser));
            BaseUserResponse userResponse = new BaseUserResponse();
            userResponse.setId(1L);
            userResponse.setFirstName("John");
            userResponse.setLastName("Doe");
            given(commonMapper.convertToResponse(any(), eq(BaseUserResponse.class))).willReturn(userResponse);

            mockMvc.perform(get("/api/v1/clubs/1/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].firstName").value("John"));
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
                            .param("q", "Running")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Running Club"));
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

            mockMvc.perform(get("/api/v1/clubs/stats")
                            .param("userId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalClubs").value(10))
                    .andExpect(jsonPath("$.userClubs").value(2))
                    .andExpect(jsonPath("$.totalMembers").value(100));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/clubs with sportType filter")
    class GetClubsBySportTypeTests {

        @Test
        @DisplayName("Should filter clubs by sport type")
        void shouldFilterClubsBySportType() throws Exception {
            Page<Club> clubPage = new PageImpl<>(List.of(testClub));
            given(clubService.getClubsBySportType(eq(ActivityType.RUN), any(Pageable.class))).willReturn(clubPage);
            given(clubService.isUserMember(1L, 1L)).willReturn(false);

            mockMvc.perform(get("/api/v1/clubs")
                            .param("userId", "1")
                            .param("sportType", "RUN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].sportType").value("RUN"));
        }
    }
}
