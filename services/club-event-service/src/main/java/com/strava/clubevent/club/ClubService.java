package com.strava.clubevent.club;

import com.strava.clubevent.common.ActivityType;
import com.strava.clubevent.exception.BadRequestException;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the monolith's ClubServiceImpl — same rules (owner/admin
 * permissions, private-club join guard, owner cannot leave) and the same
 * error messages, over id-based membership sets.
 */
@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubRepository;

    @Transactional(readOnly = true)
    public Club getClubById(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found"));
    }

    @Transactional(readOnly = true)
    public Page<Club> getAllPublicClubs(Pageable pageable) {
        return clubRepository.findPublicClubsOrderByMembersCount(pageable);
    }

    @Transactional(readOnly = true)
    public List<Club> getClubsByUserId(Long userId) {
        return clubRepository.findClubsByUserId(userId);
    }

    @Transactional
    public Club createClub(Club club, Long ownerId) {
        club.setOwnerId(ownerId);
        club.getMemberIds().add(ownerId);
        club.getAdminIds().add(ownerId);
        return clubRepository.save(club);
    }

    @Transactional
    public Club updateClub(Long clubId, Club updatedClub, Long userId) {
        Club club = getClubById(clubId);

        if (!club.getOwnerId().equals(userId) && !clubRepository.isUserAdmin(clubId, userId)) {
            throw new ForbiddenException("You don't have permission to update this club");
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

    @Transactional
    public void deleteClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);

        if (!club.getOwnerId().equals(userId)) {
            throw new ForbiddenException("Only the owner can delete the club");
        }

        clubRepository.delete(club);
    }

    @Transactional
    public void joinClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);

        if (club.isPrivate()) {
            throw new ForbiddenException("This club is private. Request to join.");
        }

        if (clubRepository.isUserMember(clubId, userId)) {
            throw new BadRequestException("You are already a member of this club");
        }

        club.getMemberIds().add(userId);
        clubRepository.save(club);
    }

    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        Club club = getClubById(clubId);

        if (club.getOwnerId().equals(userId)) {
            throw new BadRequestException("Owner cannot leave the club. Transfer ownership first.");
        }

        if (!clubRepository.isUserMember(clubId, userId)) {
            throw new BadRequestException("You are not a member of this club");
        }

        club.getMemberIds().remove(userId);
        club.getAdminIds().remove(userId);
        clubRepository.save(club);
    }

    @Transactional(readOnly = true)
    public List<Long> getClubMemberIds(Long clubId) {
        Club club = getClubById(clubId);
        return new ArrayList<>(club.getMemberIds());
    }

    @Transactional(readOnly = true)
    public boolean isUserMember(Long clubId, Long userId) {
        return clubRepository.isUserMember(clubId, userId);
    }

    @Transactional(readOnly = true)
    public Page<Club> searchClubs(String searchText, Pageable pageable) {
        return clubRepository.searchClubs(searchText, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Club> getClubsBySportType(ActivityType sportType, Pageable pageable) {
        return clubRepository.findPublicClubsBySportType(sportType, pageable);
    }

    @Transactional(readOnly = true)
    public long countPublicClubs() {
        return clubRepository.countPublicClubs();
    }

    @Transactional(readOnly = true)
    public long countTotalMembers() {
        return clubRepository.sumAllMembers();
    }
}
