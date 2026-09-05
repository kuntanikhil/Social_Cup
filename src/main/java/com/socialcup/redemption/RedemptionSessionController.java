package com.socialcup.redemption;

import com.socialcup.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RedemptionSessionController {

    private final RedemptionSessionService redemptionSessionService;

    public RedemptionSessionController(
            RedemptionSessionService redemptionSessionService
    ) {
        this.redemptionSessionService = redemptionSessionService;
    }

    @PostMapping("/cafes/{cafeId}/redemption-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateRedemptionSessionResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long cafeId,
            @Valid @RequestBody CreateRedemptionSessionRequest request
    ) {
        return redemptionSessionService.create(
                authenticatedUser.id(),
                cafeId,
                request
        );
    }

    @GetMapping("/redemption-sessions/{sessionId}")
    public RedemptionSessionResponse get(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long sessionId
    ) {
        return redemptionSessionService.get(authenticatedUser.id(), sessionId);
    }
}
