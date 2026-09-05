package com.socialcup.redemption;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RedemptionSessionRepository
        extends JpaRepository<RedemptionSession, Long> {

    List<RedemptionSession> findByMemberIdAndStatus(
            Long memberId,
            RedemptionSessionStatus status
    );

    @EntityGraph(attributePaths = {"member", "cafe", "drink"})
    @Query("select session from RedemptionSession session where session.id = :id")
    Optional<RedemptionSession> findDetailsById(@Param("id") Long id);
}
