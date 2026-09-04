package com.socialcup.discovery;

import com.socialcup.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final DiscoverService discoverService;

    public DiscoverController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @GetMapping
    public DiscoverResponse discover(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long neighbourhoodId
    ) {
        return discoverService.discover(
                authenticatedUser.id(),
                latitude,
                longitude,
                search,
                neighbourhoodId
        );
    }
}
