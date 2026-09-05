package com.socialcup.security;

import com.socialcup.user.UserRole;

public record AuthenticatedUser(Long id, String email, UserRole role) {
}
