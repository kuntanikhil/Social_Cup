package com.socialcup.neighbourhood;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NeighbourhoodService {

    private final NeighbourhoodRepository neighbourhoodRepository;

    public NeighbourhoodService(NeighbourhoodRepository neighbourhoodRepository) {
        this.neighbourhoodRepository = neighbourhoodRepository;
    }

    @Transactional(readOnly = true)
    public List<NeighbourhoodResponse> getActiveNeighbourhoods() {
        return neighbourhoodRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(neighbourhood -> new NeighbourhoodResponse(
                        neighbourhood.getId(),
                        neighbourhood.getName()
                ))
                .toList();
    }
}
