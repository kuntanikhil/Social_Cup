package com.socialcup.profile;

import com.socialcup.user.User;

public record ProfileResponse(
        Long id,
        String email,
        String displayName,
        String accountStatus
) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAccountStatus()
        );
    }
}
