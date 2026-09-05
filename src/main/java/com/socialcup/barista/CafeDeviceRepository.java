package com.socialcup.barista;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CafeDeviceRepository extends JpaRepository<CafeDevice, Long> {

    @EntityGraph(attributePaths = "cafe")
    Optional<CafeDevice> findByTokenHashAndRevokedAtIsNull(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "cafe")
    @Query("""
            select device
            from CafeDevice device
            where device.id = :id
              and device.revokedAt is null
            """)
    Optional<CafeDevice> findValidByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select device
            from CafeDevice device
            where device.cafe.id = :cafeId
              and device.revokedAt is null
            """)
    List<CafeDevice> findActiveByCafeIdForUpdate(@Param("cafeId") Long cafeId);
}
