package com.socialcup.cafe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CafeRepository extends JpaRepository<Cafe, Long> {

    List<Cafe> findByActiveTrueOrderByFeaturedDescNameAsc();

    List<Cafe> findByActiveTrueAndFeaturedTrueOrderByNameAsc();

    Optional<Cafe> findByIdAndActiveTrue(Long id);

    List<Cafe> findAllByOrderByNameAsc();
}
