package com.strava.clubevent.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMemberId> {

    Page<ClubMember> findAllByClubId(Long clubId, Pageable pageable);

    List<ClubMember> findAllByUserId(Long userId);

    long countByClubId(Long clubId);

    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    void deleteByClubIdAndUserId(Long clubId, Long userId);
}
