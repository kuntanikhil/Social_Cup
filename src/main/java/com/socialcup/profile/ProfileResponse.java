package com.socialcup.profile;

import com.socialcup.user.User;
import com.socialcup.user.UserRole;
import com.socialcup.neighbourhood.NeighbourhoodResponse;

import java.util.List;

public record ProfileResponse(
        Long id,
        String email,
        String displayName,
        String accountStatus,
        UserRole role,
        NeighbourhoodResponse homeNeighbourhood,
        List<CoffeePreferenceResponse> coffeePreferences,
        boolean onboardingCompleted
) {

    public static ProfileResponse from(User user) {
        return from(user, List.of());
    }

    public static ProfileResponse from(
            User user,
            List<CoffeePreferenceResponse> coffeePreferences
    ) {
        NeighbourhoodResponse neighbourhood = user.getHomeNeighbourhood() == null
                ? null
                : new NeighbourhoodResponse(
                        user.getHomeNeighbourhood().getId(),
                        user.getHomeNeighbourhood().getName()
                );
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAccountStatus(),
                user.getRole(),
                neighbourhood,
                coffeePreferences,
                user.getOnboardingCompletedAt() != null
        );
    }
}
