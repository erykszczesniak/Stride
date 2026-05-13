package com.strava.clubevent.club;

import com.strava.clubevent.domain.*;
import com.strava.clubevent.exception.ForbiddenException;
import com.strava.clubevent.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubService {

    private final ClubRepository clubs;
    private final ClubMemberRepository members;

    @Transactional
    public ClubDto create(Long userId, CreateClubRequest req) {
        Club c = Club.builder()
                .name(req.name())
                .description(req.description())
                .location(req.location())
                .ownerUserId(userId)
                .isPrivate(Boolean.TRUE.equals(req.isPrivate()))
                .build();
        clubs.save(c);
        members.save(ClubMember.builder()
                .clubId(c.getId())
                .userId(userId)
                .role(ClubMemberRole.OWNER)
                .build());
        return ClubDto.from(c, 1);
    }

    @Transactional(readOnly = true)
    public ClubDto get(Long id) {
        Club c = clubs.findById(id).orElseThrow(() -> new NotFoundException("Club " + id + " not found"));
        return ClubDto.from(c, members.countByClubId(id));
    }

    @Transactional(readOnly = true)
    public Page<ClubDto> search(String q, Pageable pageable) {
        return clubs.searchPublic((q == null || q.isBlank()) ? null : q.trim(), pageable)
                .map(c -> ClubDto.from(c, members.countByClubId(c.getId())));
    }

    @Transactional
    public ClubDto update(Long clubId, Long userId, UpdateClubRequest req) {
        Club c = clubs.findById(clubId).orElseThrow(() -> new NotFoundException("Club " + clubId + " not found"));
        if (!c.getOwnerUserId().equals(userId)) {
            throw new ForbiddenException("Only the club owner can modify the club");
        }
        if (req.name() != null) c.setName(req.name());
        if (req.description() != null) c.setDescription(req.description());
        if (req.location() != null) c.setLocation(req.location());
        if (req.isPrivate() != null) c.setPrivate(req.isPrivate());
        return ClubDto.from(c, members.countByClubId(clubId));
    }

    @Transactional
    public void delete(Long clubId, Long userId) {
        Club c = clubs.findById(clubId).orElseThrow(() -> new NotFoundException("Club " + clubId + " not found"));
        if (!c.getOwnerUserId().equals(userId)) {
            throw new ForbiddenException("Only the club owner can delete the club");
        }
        members.deleteAll(members.findAllByClubId(clubId, Pageable.unpaged()).getContent());
        clubs.delete(c);
    }

    @Transactional
    public void join(Long clubId, Long userId) {
        if (!clubs.existsById(clubId)) {
            throw new NotFoundException("Club " + clubId + " not found");
        }
        if (members.existsByClubIdAndUserId(clubId, userId)) return; // idempotent
        members.save(ClubMember.builder()
                .clubId(clubId)
                .userId(userId)
                .role(ClubMemberRole.MEMBER)
                .build());
    }

    @Transactional
    public void leave(Long clubId, Long userId) {
        Club c = clubs.findById(clubId).orElseThrow(() -> new NotFoundException("Club " + clubId + " not found"));
        if (c.getOwnerUserId().equals(userId)) {
            throw new ForbiddenException("The owner cannot leave their own club — delete it or transfer ownership");
        }
        members.deleteByClubIdAndUserId(clubId, userId);
    }

    @Transactional(readOnly = true)
    public Page<ClubMember> listMembers(Long clubId, Pageable pageable) {
        return members.findAllByClubId(clubId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ClubMember> myClubs(Long userId) {
        return members.findAllByUserId(userId);
    }
}
