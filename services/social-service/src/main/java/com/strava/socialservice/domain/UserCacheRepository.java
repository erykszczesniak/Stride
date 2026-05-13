package com.strava.socialservice.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserCacheRepository extends JpaRepository<UserCache, Long> {

    List<UserCache> findAllByUserIdIn(Collection<Long> userIds);
}
