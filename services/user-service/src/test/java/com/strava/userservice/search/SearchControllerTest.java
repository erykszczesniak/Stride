package com.strava.userservice.search;

import com.strava.userservice.config.SecurityConfig;
import com.strava.userservice.domain.AuthProvider;
import com.strava.userservice.domain.Role;
import com.strava.userservice.domain.User;
import com.strava.userservice.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the global search surface ({@code /api/v1/search}). */
@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
@DisplayName("SearchController")
class SearchControllerTest {

    private static final String USER_ID = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private User jane() {
        return User.builder()
                .id(3L).firstName("Jane").lastName("Smith").email("jane@example.com")
                .authProvider(AuthProvider.LOCAL).role(Role.USER).active(true)
                .build();
    }

    @Test
    void globalSearchReturnsUsersAndEmptyOtherEntityLists() throws Exception {
        given(userRepository.searchUsersByText(eq("jane"), eq(PageRequest.of(0, 5))))
                .willReturn(new PageImpl<>(List.of(jane())));

        mockMvc.perform(get("/api/v1/search").param("query", "jane").header(USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(3))
                .andExpect(jsonPath("$.users[0].fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.activities").isEmpty())
                .andExpect(jsonPath("$.clubs").isEmpty())
                .andExpect(jsonPath("$.challenges").isEmpty())
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    void globalSearchHonorsCustomLimit() throws Exception {
        given(userRepository.searchUsersByText(eq("jane"), eq(PageRequest.of(0, 2))))
                .willReturn(new PageImpl<>(List.of(jane())));

        mockMvc.perform(get("/api/v1/search")
                        .param("query", "jane").param("limit", "2").header(USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].id").value(3));
    }

    @Test
    void userSearchDefaultsToLimit10() throws Exception {
        given(userRepository.searchUsersByText(eq("jane"), eq(PageRequest.of(0, 10))))
                .willReturn(new PageImpl<>(List.of(jane())));

        mockMvc.perform(get("/api/v1/search/users").param("query", "jane").header(USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Jane"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void perEntityEndpointsOwnedByOtherServicesReturnEmptyLists() throws Exception {
        for (String entity : List.of("activities", "clubs", "challenges", "events")) {
            mockMvc.perform(get("/api/v1/search/" + entity).param("query", "x").header(USER_ID, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Test
    void searchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("query", "jane"))
                .andExpect(status().isUnauthorized());
    }
}
