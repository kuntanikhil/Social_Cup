package com.socialcup.cafe;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CafeRepository extends JpaRepository<Cafe, Long> {

    @EntityGraph(attributePaths = "neighbourhood")
    List<Cafe> findByActiveTrueOrderByFeaturedDescNameAsc();

    @EntityGraph(attributePaths = "neighbourhood")
    List<Cafe> findByActiveTrueOrderByNameAsc();

    List<Cafe> findByActiveTrueAndFeaturedTrueOrderByNameAsc();

    Optional<Cafe> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cafe from Cafe cafe where cafe.id = :id")
    Optional<Cafe> findByIdForUpdate(@Param("id") Long id);

    List<Cafe> findAllByOrderByNameAsc();
}
