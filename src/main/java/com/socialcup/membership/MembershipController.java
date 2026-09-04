package com.socialcup.membership;

import com.socialcup.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public MembershipResponse getMembership(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return membershipService.getMembership(authenticatedUser.id());
    }

    // TODO Remove or disable this temporary endpoint after Stripe activation is implemented.
    @PostMapping("/demo-activate")
    public MembershipResponse demoActivate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return membershipService.demoActivate(authenticatedUser.id());
    }
}
