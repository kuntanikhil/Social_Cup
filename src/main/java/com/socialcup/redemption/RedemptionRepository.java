package com.socialcup.redemption;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    @EntityGraph(attributePaths = {"member", "cafe", "drink"})
    List<Redemption> findAllByOrderByRedeemedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"member", "drink"})
    List<Redemption> findByCafeIdAndStatusAndRedeemedAtGreaterThanEqualAndRedeemedAtLessThanOrderByRedeemedAtDesc(
            Long cafeId,
            RedemptionStatus status,
            OffsetDateTime periodStart,
            OffsetDateTime periodEnd
    );
}
