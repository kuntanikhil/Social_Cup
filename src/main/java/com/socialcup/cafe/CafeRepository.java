package com.socialcup.cafe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface CafeRepository extends JpaRepository<Cafe, Long> {

    @EntityGraph(attributePaths = "neighbourhood")
    List<Cafe> findByActiveTrueOrderByFeaturedDescNameAsc();

    @EntityGraph(attributePaths = "neighbourhood")
    List<Cafe> findByActiveTrueOrderByNameAsc();

    List<Cafe> findByActiveTrueAndFeaturedTrueOrderByNameAsc();

    Optional<Cafe> findByIdAndActiveTrue(Long id);

    List<Cafe> findAllByOrderByNameAsc();
}
