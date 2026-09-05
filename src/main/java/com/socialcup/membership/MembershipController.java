package com.socialcup.membership;

import com.socialcup.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final StripeCheckoutService stripeCheckoutService;

    public MembershipController(
            MembershipService membershipService,
            StripeCheckoutService stripeCheckoutService
    ) {
        this.membershipService = membershipService;
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @GetMapping
    public MembershipResponse getMembership(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return membershipService.getMembership(authenticatedUser.id());
    }

    // TODO DEVELOPMENT ONLY: remove or disable this fallback before production launch.
    @PostMapping("/demo-activate")
    public MembershipResponse demoActivate(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return membershipService.demoActivate(authenticatedUser.id());
    }

    @PostMapping("/checkout")
    public StripeCheckoutResponse checkout(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return stripeCheckoutService.createCheckout(authenticatedUser.id());
    }

    // TODO DEVELOPMENT ONLY: remove this recovery fallback after webhook operations mature.
    @PostMapping("/reconcile")
    public MembershipResponse reconcile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return membershipService.reconcile(authenticatedUser.id());
    }

    @ExceptionHandler(StripeCheckoutException.class)
    public ResponseEntity<StripeCheckoutErrorResponse> handleStripeCheckoutFailure(
            StripeCheckoutException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new StripeCheckoutErrorResponse(
                        exception.getMessage(),
                        exception.getStage(),
                        exception.getStripeCode(),
                        exception.getStripeRequestId()
                ));
    }
}
