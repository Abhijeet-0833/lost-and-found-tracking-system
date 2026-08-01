package com.lostfound.repository;

import com.lostfound.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    
    @Query("SELECT m FROM Match m WHERE m.lostItem.itemId = :lostId AND m.foundItem.itemId = :foundId")
    Match findByItems(@Param("lostId") Long lostId, @Param("foundId") Long foundId);

    @Query("SELECT m FROM Match m WHERE m.lostItem.reportedBy = :userId OR m.foundItem.reportedBy = :userId")
    List<Match> findMatchesForUser(@Param("userId") Long userId);
}
