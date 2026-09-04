package com.socialcup.neighbourhood;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/neighbourhoods")
public class NeighbourhoodController {

    private final NeighbourhoodService neighbourhoodService;

    public NeighbourhoodController(NeighbourhoodService neighbourhoodService) {
        this.neighbourhoodService = neighbourhoodService;
    }

    @GetMapping
    public List<NeighbourhoodResponse> getNeighbourhoods() {
        return neighbourhoodService.getActiveNeighbourhoods();
    }
}
