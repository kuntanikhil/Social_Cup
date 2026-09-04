package com.socialcup.profile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coffee-preferences")
public class CoffeePreferenceController {

    private final ProfileService profileService;

    public CoffeePreferenceController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public List<CoffeePreferenceResponse> getCoffeePreferences() {
        return profileService.getActiveCoffeePreferences();
    }
}
