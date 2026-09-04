package com.socialcup.profile;

public record CoffeePreferenceResponse(
        Long id,
        String code,
        String displayName
) {

    public static CoffeePreferenceResponse from(CoffeePreference preference) {
        return new CoffeePreferenceResponse(
                preference.getId(),
                preference.getCode(),
                preference.getDisplayName()
        );
    }
}
