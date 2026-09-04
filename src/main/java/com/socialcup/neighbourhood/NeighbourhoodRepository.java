package com.socialcup.neighbourhood;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NeighbourhoodRepository extends JpaRepository<Neighbourhood, Long> {

    List<Neighbourhood> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<Neighbourhood> findByIdAndActiveTrue(Long id);
}
