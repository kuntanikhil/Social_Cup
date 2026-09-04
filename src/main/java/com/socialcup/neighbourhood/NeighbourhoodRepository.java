package com.socialcup.neighbourhood;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NeighbourhoodRepository extends JpaRepository<Neighbourhood, Long> {

    List<Neighbourhood> findByActiveTrueOrderBySortOrderAscNameAsc();
}
