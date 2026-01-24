package com.strava.backend.service.impl;

import com.strava.backend.domain.Club;
import com.strava.backend.domain.User;
import com.strava.backend.exception.ApiRequestException;
import com.strava.backend.repository.ClubRepository;
import com.strava.backend.repository.UserRepository;
import com.strava.backend.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.strava.backend.constants.ErrorMessage.*;

@Service
@RequiredArgsConstructor
public class ClubServiceImpl implements ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    @Override
    public Club getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ApiRequestException("Club not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public Page<Club> getAllPublicClubs(Pageable pageable) {
        return clubRepository.findPublicClubsOrderByMembersCount(pageable);
    }

    @Override
    public List<Club> getClubsByUserId(Long userId) {
        return clubRepository.findClubsByUserId(userId);
    }

    @Override
    public List<Club> getClubsOwnedByUser(Long userId) {
        return clubRepository.findByOwnerId(userId);
    }

    @Override
    @Transactional
    public Club createClub(Club club, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        club.setOwner(owner);
        club.getMembers().add(owner);
        club.getAdmins().add(owner);

        return clubRepository.save(club);
    }

    @Override
    @Transactional
    public Club updateClub(Long clubId, Club updatedClub, Long userId) {
        Club club = getClubById(clubId);

        if (!club.getOwner().getId().equals(userId) && !clubRepository.isUserAdmin(clubId, userId)) {
            throw new ApiRequestException("You don't have permission to update this club", HttpStatus.FORBIDDEN);
        }

        club.setName(updatedClub.getName());
        club.setDescription(updatedClub.getDescription());
        club.setCity(updatedClub.getCity());
        club.setCountry(updatedClub.getCountry());
        club.setSportType(updatedClub.getSportType());
        club.setWebsite(updatedClub.getWebsite());
        club.setPrivate(updatedClub.isPrivate());

        if (updatedClub.getCoverImage() != null) {
            club.setCoverImage(updatedClub.getCoverImage());
        }
        if (updatedClub.getProfileImage() != null) {
            club.setProfileImage(updatedClub.getProfileImage());
        }

        return clubRepository.save(club);
    }

    @Override
    @Transactional
    public void deleteClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);

        if (!club.getOwner().getId().equals(userId)) {
            throw new ApiRequestException("Only the owner can delete the club", HttpStatus.FORBIDDEN);
        }

        clubRepository.delete(club);
    }

    @Override
    @Transactional
    public void joinClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (club.isPrivate()) {
            throw new ApiRequestException("This club is private. Request to join.", HttpStatus.FORBIDDEN);
        }

        if (clubRepository.isUserMember(clubId, userId)) {
            throw new ApiRequestException("You are already a member of this club", HttpStatus.BAD_REQUEST);
        }

        club.getMembers().add(user);
        clubRepository.save(club);
    }

    @Override
    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(USER_NOT_FOUND, HttpStatus.NOT_FOUND));

        if (club.getOwner().getId().equals(userId)) {
            throw new ApiRequestException("Owner cannot leave the club. Transfer ownership first.", HttpStatus.BAD_REQUEST);
        }

        if (!clubRepository.isUserMember(clubId, userId)) {
            throw new ApiRequestException("You are not a member of this club", HttpStatus.BAD_REQUEST);
        }

        club.getMembers().remove(user);
        club.getAdmins().remove(user);
        clubRepository.save(club);
    }

    @Override
    public List<?> getClubMembers(Long clubId) {
        Club club = getClubById(clubId);
        return new ArrayList<>(club.getMembers());
    }

    @Override
    public boolean isUserMember(Long clubId, Long userId) {
        return clubRepository.isUserMember(clubId, userId);
    }

    @Override
    public Page<Club> searchClubs(String searchText, Pageable pageable) {
        return clubRepository.searchClubs(searchText, pageable);
    }
}
