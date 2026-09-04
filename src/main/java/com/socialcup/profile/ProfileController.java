package com.socialcup.profile;

import com.socialcup.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse getProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return profileService.getProfile(authenticatedUser.id());
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return profileService.updateProfile(authenticatedUser.id(), request);
    }
}
