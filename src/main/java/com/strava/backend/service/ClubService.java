package com.strava.backend.service;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClubService {

    Club getClubById(Long clubId);

    Page<Club> getAllPublicClubs(Pageable pageable);

    List<Club> getClubsByUserId(Long userId);

    List<Club> getClubsOwnedByUser(Long userId);

    Club createClub(Club club, Long ownerId);

    Club updateClub(Long clubId, Club club, Long userId);

    void deleteClub(Long clubId, Long userId);

    void joinClub(Long clubId, Long userId);

    void leaveClub(Long clubId, Long userId);

    List<User> getClubMembers(Long clubId);

    boolean isUserMember(Long clubId, Long userId);

    Page<Club> searchClubs(String searchText, Pageable pageable);

    Page<Club> getClubsBySportType(com.strava.backend.enums.ActivityType sportType, Pageable pageable);

    long countPublicClubs();

    long countTotalMembers();
}
